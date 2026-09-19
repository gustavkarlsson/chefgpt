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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.toTools

private val logger = LoggerFactory.getLogger("KoogIngredientScanAgent")

private val SYSTEM_PROMPT =
    """
    You are an ingredient scanner. Your only job is to look at the images and
    identify the edible food ingredients that are visible in them.
    Ignore non-food objects, such as packaging, backgrounds, utensils and people.

    Workflow:
    1. Call getIngredients to fetch existing ingredients.
    2. Identify all distinct food ingredients from the images.
    3. Call addIngredients to save the identified ingredients.

    Ingredient naming:
    Copy the spelling of the existing ingredients if they can be found (including capitalization), to avoid duplicates.
    For new ingredients, use simple, plain-text lowercase names (e.g. "tomatoes", "eggs", "milk", "black pepper").
    """.trimIndent()

class KoogIngredientScanAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val ingredientStore: IngredientStore,
) : IngredientScanAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<String>? =
        try {
            val agent = buildAgent(userId, buildPrompt(images), ingredientScanStrategy())
            agent.run("Scan these images for ingredients and add the ones you find.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to scan ingredients", e)
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
        // The only tools the scanner can reach are the ingredient store's.
        toolRegistry =
            ToolRegistry {
                tools(ingredientStore.toTools(userId))
            },
    )
}

private fun buildPrompt(images: List<UploadedFile>) =
    prompt("scan-ingredients") {
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
private fun ingredientScanStrategy() =
    scanStrategy("scan-ingredients") { call ->
        if (call.tool == "addIngredients") {
            call.argsJson["ingredients"]
                ?.jsonArray
                ?.mapNotNull { element -> element.jsonPrimitive.takeIf { it.isString }?.contentOrNull }
                .orEmpty()
        } else {
            emptyList()
        }
    }
