package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.ktor.aiAgent
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import io.ktor.server.routing.RoutingContext
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.toTools
import se.gustavkarlsson.chefgpt.recipes.RecipeClient

class KoogAiAgent(
    private val recipeClient: RecipeClient,
    private val ingredientStore: IngredientStore,
) : AiAgent {
    override suspend fun RoutingContext.run(
        userId: UserId,
        chatId: ChatId,
    ) {
        val agent =
            aiAgent(
                strategy = findRecipeStrategy(),
                model = AnthropicModels.Haiku_4_5,
                tools =
                    ToolRegistry {
                        // TODO Tools should really be set in the plugin config, but it's broken due to https://github.com/JetBrains/koog/issues/1705
                        tools(ingredientStore.toTools(userId))
                        tools(recipeClient.asTools())
                    },
            )
        agent.run(Unit, chatId.value.toString())
    }
}
