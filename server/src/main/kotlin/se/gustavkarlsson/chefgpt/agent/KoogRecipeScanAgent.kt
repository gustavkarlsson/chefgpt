package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgentFunctionalStrategy
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeStore
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
    - If a photo depicts an image of an identified dish. Use that as its image URL.
    - If the photo contains things that is not the dish (such as text, empty space, etc.),
      use the cropImage tool to crop out the unwanted parts and use the resulting image URL.
    - If the photo fully depicts the dish and nothing more, use it as-is.
    - If no such photo can be found, leave the image URL null.

    Do not modify, delete, or look up existing recipes.
    """.trimIndent()

// TODO Image cropper!
class KoogRecipeScanAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val recipeStore: RecipeStore,
    private val recipeLookup: RecipeLookup,
) : RecipeScanAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<String>? =
        try {
            val agent = buildAgent(userId, buildPrompt(images), recipeScanStrategy())
            return agent.run("Scan these photos for recipes and save the ones you find.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to scan recipes", e)
            null
        }

    private fun buildAgent(
        userId: UserId,
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
        // The only tools the scanner can reach are the recipe store's.
        toolRegistry =
            ToolRegistry {
                tools(recipeStore.toTools(userId, recipeLookup))
            },
    )
}

private fun buildPrompt(images: List<UploadedFile>) =
    prompt("scan-recipes") {
        system(SYSTEM_PROMPT)
        user {
            for (image in images) {
                image.toImageAttachmentOrNull()?.let {
                    image(it)
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
