package se.gustavkarlsson.chefgpt.agent.saverecipes

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.facts.toMeasurementPromptText
import se.gustavkarlsson.chefgpt.files.FileKind
import se.gustavkarlsson.chefgpt.files.ImageCropper
import se.gustavkarlsson.chefgpt.files.ImageEditTools
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.files.fileKindOrNull
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.toTools
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
    private val recipeLookup: RecipeLookup,
    private val imageCropper: ImageCropper,
    private val factRepository: FactRepository,
) : SaveRecipesAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<RecipeId> {
        val facts = factRepository.getFacts(userId)
        val recordingSaveRecipeTool =
            TODO("Create a tool for the RecipeRepostitory.saveRecipe call that records every successfully created ID")
        val agent = buildAgent(userId, images, recordingSaveRecipeTool, buildPrompt(images, facts))
        agent.run("Scan these photos for recipes and save the ones you find.")
        return recordingSaveRecipeTool.savedIds.toList()
    }

    private fun buildAgent(
        userId: UserId,
        images: List<UploadedFile>,
        repository: RecipeRepository,
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
                tools(repository.toTools(userId, recipeLookup))
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
