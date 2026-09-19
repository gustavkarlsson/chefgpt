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

    Read out the title, ingredients, steps, times and any description you can
    actually see, and leave out whatever is missing rather than filling it in
    yourself. Never invent ingredients or steps. If something is unreadable,
    report an error instead of guessing.

    A recipe's photo should look like a picture of food, never like a page of
    text. Only set imageUrl when one of the pictures shows that recipe's dish,
    and leave it empty otherwise.

    Do not call any other tool. Do not modify, delete, or look up existing
    recipes.

    REQUIREMENTS on the response:
    - Write exactly ONE single response message and only when you are finished
    - Plain text
    - Each saved recipe's title on a separate line.
    - No extra blank lines before, after, between, or within a recipe title.
    - If no recipes were identified, a single blank line is a valid response.
    YOU MUST ADHERE STRICTLY TO THESE RULES
    """.trimIndent()

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
            val prompt =
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
            val agent =
                AIAgent(
                    promptExecutor = promptExecutor,
                    agentConfig =
                        AIAgentConfig(
                            prompt = prompt,
                            model = model,
                            maxAgentIterations = 10,
                        ),
                    // The only tools the scanner can reach are the recipe store's.
                    toolRegistry =
                        ToolRegistry {
                            tools(recipeStore.toTools(userId, recipeLookup))
                        },
                )
            val rawResponse = agent.run("Scan these photos for recipes and save the ones you find.")
            return rawResponse.lines()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to scan recipes", e)
            null
        }
}
