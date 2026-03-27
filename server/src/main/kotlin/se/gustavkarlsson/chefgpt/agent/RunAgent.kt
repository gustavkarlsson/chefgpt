package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.ktor.aiAgent
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.routing.RoutingContext
import org.jetbrains.exposed.v1.jdbc.Database
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.tools.PostgresIngredientStore
import se.gustavkarlsson.chefgpt.tools.SpoonacularClient

suspend fun RoutingContext.runAgent(
    userId: UserId,
    chatId: ChatId,
) {
    val db: Database by call.application.dependencies
    val spoonacularClient: SpoonacularClient by call.application.dependencies
    val agent =
        aiAgent(
            strategy = findRecipeStrategy(),
            model = AnthropicModels.Haiku_4_5,
            tools =
                ToolRegistry {
                    // TODO Tools should really be set in the plugin config, but it's broken due to https://github.com/JetBrains/koog/issues/1705
                    tools(PostgresIngredientStore(db, userId).asTools())
                    tools(spoonacularClient.asTools())
                },
        )
    agent.run(Unit, chatId.value.toString())
}
