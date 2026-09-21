package se.gustavkarlsson.chefgpt.agent.saverecipes

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.api.toSummary
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.facts.toMeasurementPromptText
import se.gustavkarlsson.chefgpt.files.FileKind
import se.gustavkarlsson.chefgpt.files.ImageCropper
import se.gustavkarlsson.chefgpt.files.ImageEditTools
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.files.fileKindOrNull
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.createRecipe
import se.gustavkarlsson.chefgpt.toImageAttachmentOrNull

private val SYSTEM_PROMPT =
    """
    You are a recipe scanner. Your only job is to read the recipe or recipes in
    the photos and save them to the user's recipes.

    Read every photo carefully. Decide whether the photos show one recipe — a
    single recipe is often spread over several pages, so photos that continue
    one another are one recipe — or several distinct recipes. Save each recipe
    with its own createRecipe call.

    Some recipes are variants of another recipe in the same set of photos. For
    example, one photo shows a base pancake recipe and another a blueberry
    pancake recipe that says "use the base pancake recipe but reduce the milk
    to 3 dl". When a recipe references another recipe that is also in the
    photos:

    - Save the base recipe on its own, exactly as written.
    - Save the variant as a complete recipe on its own too. Start from the
      base recipe's full contents — ingredients, steps, times, servings — and
      apply the variant's stated changes on top, with the variant overriding
      the base wherever they differ. The variant must stand alone and be
      readable without the base, so never save a variant that only says "see
      the base recipe" when the base recipe is in the photos.

    If a recipe references a base recipe that is not in the photos, you cannot
    merge it. Save whatever you can read from the photo and keep the reference
    to the base recipe in the variant's own text (for example as a step),
    rather than inventing the base's contents.

    Extract as many values you can to match the createRecipe tool parameters.
    Don't invent data. Leave out anything that is is missing rather than filling
    it in yourself.

    Convert the recipe's measurements to the user's preferences (shown below):
    apply their measurement preference to compressible dry goods, viscous or
    sticky liquids, and irregular solids. Easy-to-pour liquids stay volume, and
    amounts not given as weight or volume (cloves, pinches, dashes) stay as
    written. Where a preference is unknown, keep the recipe's original
    measurement.

    *Note: From now on, the word "dish" means whatever the recipe makes,
    whether it's food, baked items, beverages, etc.*

    Recipe image URL rules:
    Each photo is shown together with its url.

    - If a photo depicts an image of an identified dish, use its url as the recipe's image URL.
    - If the photo contains things that are not the dish (text, empty space, etc.),
      determine the interesting area and call cropImage with the photo's url and use the resulting url.
    - If the photo fully depicts the dish and nothing more, use its url as-is.
    - If no such photo can be found, leave the image URL null.

    Do not modify, delete, or look up existing recipes.
    """.trimIndent()

class KoogSaveRecipesAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val recipeRepository: RecipeRepository,
    private val imageCropper: ImageCropper,
    private val factRepository: FactRepository,
) : SaveRecipesAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<RecipeId> {
        val createRecipeTool = RecordingCreateRecipeTool(recipeRepository, userId)
        val facts = factRepository.getFacts(userId)
        val agent = buildAgent(images, createRecipeTool, buildPrompt(images, facts))
        agent.run("Scan these photos for recipes and save the ones you find.")
        return createRecipeTool.savedIds.toList()
    }

    private fun buildAgent(
        images: List<UploadedFile>,
        tool: ToolSet,
        prompt: Prompt,
    ) = AIAgent(
        promptExecutor = promptExecutor,
        agentConfig =
            AIAgentConfig(
                prompt = prompt,
                model = model,
                maxAgentIterations = 10,
            ),
        strategy = saveRecipesStrategy(),
        toolRegistry =
            ToolRegistry {
                tools(tool)
                tools(
                    ImageEditTools(imageCropper) {
                        images
                            .filter { fileKindOrNull(it.mimeType) == FileKind.Image }
                            .map { it.url }
                    },
                )
            },
    )
}

// Exposes only createRecipe, and records every successfully created ID so scan can report exactly what it added.
private class RecordingCreateRecipeTool(
    private val store: RecipeRepository,
    private val userId: UserId,
) : ToolSet {
    val savedIds: Set<RecipeId>
        field = mutableSetOf()

    @Suppress("unused")
    @Tool
    @LLMDescription(
        "Write a recipe of your own into the user's recipes, for example one you read in a photo " +
            "or a document they shared. Fit what you can read into the fields and leave out what " +
            "is missing — never invent ingredients or steps.",
    )
    suspend fun createRecipe(
        @LLMDescription("The name of the dish.")
        title: String,
        @LLMDescription("The instructions, one per step.")
        steps: List<String>,
        @LLMDescription("The ingredients, or an empty list if they are unknown.")
        ingredients: List<ApiRecipeIngredient> = emptyList(),
        @LLMDescription("The nutrients, or an empty list if they are unknown.")
        nutrients: List<ApiNutrient> = emptyList(),
        @LLMDescription("A short summary of the dish, or an empty string to leave it out.")
        description: String = "",
        @LLMDescription(
            "The url of a picture to use as the recipe's photo: from listSharedFiles when the " +
                "picture is nothing but the food, and from cropImage when it also holds writing " +
                "or background. Empty string for no photo.",
        )
        imageUrl: String = "",
        @LLMDescription("The preparation time in minutes, or 0 if it is unknown.")
        preparationMinutes: Int = 0,
        @LLMDescription("The cooking time in minutes, or 0 if it is unknown.")
        cookingMinutes: Int = 0,
        @LLMDescription("The total time in minutes, or 0 if it is unknown.")
        totalMinutes: Int = 0,
        @LLMDescription(
            "The lower end of how many servings the recipe makes, for example 4 for \"4-6 servings\" " +
                "or 4 for an exact \"4 servings\". Must be given together with maxServings, " +
                "or the servings are left out.",
        )
        minServings: Int = 0,
        @LLMDescription(
            "The upper end of how many servings the recipe makes, for example 6 for \"4-6 servings\" " +
                "or 4 for an exact \"4 servings\" (same as minServings). Must be given together with " +
                "minServings, or the servings are left out.",
        )
        maxServings: Int = 0,
    ): ApiRecipeSummary {
        val recipe =
            store.createRecipe(
                userId,
                title,
                steps,
                ingredients,
                nutrients,
                description,
                imageUrl,
                preparationMinutes,
                cookingMinutes,
                totalMinutes,
                minServings,
                maxServings,
            )
        savedIds += recipe.id
        return recipe.toSummary()
    }
}

private fun buildPrompt(
    images: List<UploadedFile>,
    facts: UserFacts,
) = prompt("save-recipes") {
    system(SYSTEM_PROMPT + "\n\n" + facts.toMeasurementPromptText())
    user {
        for (image in images) {
            image.toImageAttachmentOrNull()?.let { attachment ->
                text("Photo url: ${image.url}")
                image(attachment)
            }
        }
    }
}

private fun saveRecipesStrategy() =
    functionalStrategy<String, Unit>("save-recipes") { input ->
        var message = requestLLM(input)
        repeat(config.maxAgentIterations) {
            val toolCalls = getToolCalls(message)
            if (toolCalls.isEmpty()) {
                return@repeat
            }
            val toolResults = executeTools(toolCalls)
            message = sendToolResults(toolResults)
        }
    }
