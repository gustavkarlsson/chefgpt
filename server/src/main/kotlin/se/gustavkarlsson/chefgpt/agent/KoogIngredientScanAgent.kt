package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.ktor.llm
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.ktor.server.routing.RoutingContext
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.toTools

private val SYSTEM_PROMPT =
    """
    You are an ingredient scanner. Your only job is to look at the image and
    identify the edible food ingredients that are visible in it.

    Identify every distinct food ingredient you can see, using simple, singular,
    lowercase names (e.g. "tomato", "egg", "milk"). Ignore non-food objects,
    packaging, backgrounds, utensils and people.

    Add all identified ingredients to the user's inventory in a single call to the
    addIngredients tool. Do not remove, delete or look up anything, and do not
    call any other tool.

    When you are done, reply with exactly one line and nothing else:
    - "OK: <count>" where <count> is the number of ingredients you found in the
      image, even if it is 0, and regardless of how many were newly added. An
      image with no food in it is not an error: report it as "OK: 0".
    - "ERROR: <reason>" if anything technical prevents you from analyzing the
      image, for example the image is missing, corrupt, or cannot be loaded.
    """.trimIndent()

class KoogIngredientScanAgent(
    private val ingredientStore: IngredientStore,
) : IngredientScanAgent {
    override suspend fun RoutingContext.scan(
        userId: UserId,
        imageUrl: ImageUrl,
    ): Result<Int, String> {
        val format =
            imageUrl.value
                .substringAfterLast('.')
                .substringBefore('?')
                .ifEmpty { "jpeg" }
        val agent =
            AIAgent(
                promptExecutor = llm(),
                agentConfig =
                    AIAgentConfig(
                        prompt =
                            prompt("scan-ingredients") {
                                system(SYSTEM_PROMPT)
                                user {
                                    image(AttachmentSource.Image(AttachmentContent.URL(imageUrl.value), format))
                                }
                            },
                        model = AnthropicModels.Haiku_4_5,
                        maxAgentIterations = 10,
                    ),
                // The only tools the scanner can reach are the ingredient store's.
                toolRegistry =
                    ToolRegistry {
                        tools(ingredientStore.toTools(userId))
                    },
            )
        val reply = agent.run("Scan this image for ingredients and add the ones you find.")
        return parseScanResult(reply)
    }
}

private fun parseScanResult(reply: String): Result<Int, String> {
    val trimmed = reply.trim()
    return when {
        trimmed.startsWith("OK:", ignoreCase = true) -> {
            when (val count = trimmed.substringAfter(':').trim().toIntOrNull()) {
                null -> Err("Could not parse ingredient count from reply: $trimmed")
                else -> Ok(count)
            }
        }

        trimmed.startsWith("ERROR:", ignoreCase = true) -> {
            Err(trimmed.substringAfter(':').trim())
        }

        else -> {
            Err("Unexpected agent reply: $trimmed")
        }
    }
}
