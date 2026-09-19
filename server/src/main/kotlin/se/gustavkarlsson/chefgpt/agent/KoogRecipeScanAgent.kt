package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.format
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeStore
import se.gustavkarlsson.chefgpt.recipes.toTools

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

    When you are done, reply with exactly one line and nothing else:
    - "OK: <count>" where <count> is the number of recipes you saved.
    - "ERROR: <reason>" if anything technical prevents you from reading the
      photos, for example an image is missing, corrupt, or cannot be loaded.
    """.trimIndent()

class KoogRecipeScanAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val recipeStore: RecipeStore,
    private val recipeLookup: RecipeLookup,
) : RecipeScanAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<ApiAttachment>,
    ): Result<List<ApiRecipeSummary>, String> {
        val knownIds = recipeStore.getRecipeSummaries(userId).mapTo(mutableSetOf()) { it.id }
        val prompt =
            prompt("scan-recipes") {
                system(SYSTEM_PROMPT)
                user {
                    images.forEach { image ->
                        image(
                            AttachmentSource.Image(
                                AttachmentContent.URL(image.url),
                                image.format,
                                image.mimeType,
                                image.fileName,
                            ),
                        )
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
        val reply = agent.run("Scan these photos for recipes and save the ones you find.")
        // The tool results go back to the LLM, not to us, so the saved recipes are
        // found by diffing the store against what was there before the run.
        return parseScanResult(reply).map { _ ->
            recipeStore.getRecipeSummaries(userId).filter { it.id !in knownIds }
        }
    }
}
