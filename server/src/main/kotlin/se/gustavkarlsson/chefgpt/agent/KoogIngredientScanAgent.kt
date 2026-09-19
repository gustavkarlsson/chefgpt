package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.CancellationException
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

    First, look up existing ingredients the user already has with the getIngredients tool.

    Then, identify and name every distinct food ingredient you can see in the photos.
    Copy the spelling of the existing ingredients if they can be found, to avoid duplicates.
    For new ingredients, use simple, lowercase names (e.g. "tomatoes", "eggs", "milk", "black pepper").
    Ignore non-food objects, such as packaging, backgrounds, utensils and people.

    Add all identified ingredients to the user's inventory with the addIngredients tool.
    Do not remove or delete any ingredients, and do not call any other tools.

    REQUIREMENTS on the response:
    - Write exactly ONE single response message and only when you are finished
    - Plain text
    - Each identified ingredient on a separate line (whether it existed or not).
    - No extra blank lines before, after, between, or between ingredients.
    - If no ingredients were identified, a single blank line is a valid response.
    YOU MUST ADHERE STRICTLY TO THESE RULES
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
            val agent =
                AIAgent(
                    promptExecutor = promptExecutor,
                    agentConfig =
                        AIAgentConfig(
                            prompt =
                                prompt("scan-ingredients") {
                                    system(SYSTEM_PROMPT)
                                    user {
                                        for (image in images) {
                                            image.toImageAttachmentOrNull()?.let {
                                                image(it)
                                            }
                                        }
                                    }
                                },
                            model = model,
                            maxAgentIterations = 10,
                        ),
                    // The only tools the scanner can reach are the ingredient store's.
                    toolRegistry =
                        ToolRegistry {
                            tools(ingredientStore.toTools(userId))
                        },
                )
            val rawResponse = agent.run("Scan these images for ingredients and add the ones you find.")
            rawResponse.lines()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to scan ingredients", e)
            null
        }
}
