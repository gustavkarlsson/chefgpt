package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgentFunctionalStrategy
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.FileKind
import se.gustavkarlsson.chefgpt.files.ImageCropper
import se.gustavkarlsson.chefgpt.files.ImageEditTools
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.files.fileKindOrNull
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.toTools

private val logger = LoggerFactory.getLogger("KoogRecipeScanAgent")

private val SYSTEM_PROMPT =
    """
    You are a recipe scanner. Your only job is to read the recipe or recipes in
    the photos and save them to the user's recipes.

    Read every photo carefully. Decide whether the photos show one recipe — a
    single recipe is often spread over several pages, so photos that continue
    one another are one recipe — or several distinct recipes. Save each recipe
    with its own createRecipe call.

    Extract as many values you can to match the createRecipe tool parameters.
    Don't invent data. Leave out anything that is is missing rather than filling
    it in yourself.

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

class KoogRecipeScanAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val recipeRepository: RecipeRepository,
    private val recipeLookup: RecipeLookup,
    private val imageCropper: ImageCropper,
) : RecipeScanAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<String> {
        val agent = buildAgent(userId, images, buildPrompt(images), recipeScanStrategy())
        return agent.run("Scan these photos for recipes and save the ones you find.")
    }

    private fun buildAgent(
        userId: UserId,
        images: List<UploadedFile>,
        prompt: Prompt,
        strategy: AIAgentFunctionalStrategy<String, List<String>>,
    ) = AIAgent(
        promptExecutor = promptExecutor,
        agentConfig =
            AIAgentConfig(
                prompt = prompt,
                model = model,
                maxAgentIterations = 10,
            ),
        strategy = strategy,
        toolRegistry =
            ToolRegistry {
                tools(recipeRepository.toTools(userId, recipeLookup))
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

private fun buildPrompt(images: List<UploadedFile>) =
    prompt("scan-recipes") {
        system(SYSTEM_PROMPT)
        user {
            for (image in images) {
                image.toImageAttachmentOrNull()?.let { attachment ->
                    text("Photo url: ${image.url}")
                    image(attachment)
                }
            }
        }
    }

// TODO Fix brittle parsing since the tool signature might change
private fun recipeScanStrategy() =
    scanStrategy("scan-recipes") { call ->
        if (call.tool == "createRecipe") {
            val titles =
                call.argsJson["title"]
                    ?.jsonPrimitive
                    ?.takeIf { it.isString }
                    ?.contentOrNull
            listOfNotNull(titles)
        } else {
            emptyList()
        }
    }
