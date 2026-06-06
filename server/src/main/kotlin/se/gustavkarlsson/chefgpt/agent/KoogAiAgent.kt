package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.ktor.aiAgent
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import io.ktor.server.routing.RoutingContext
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chats.ChatNamingTools
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.toTools

class KoogAiAgent(
    private val ingredientStore: IngredientStore,
    private val chatRepository: ChatRepository,
    private val eventRepository: EventRepository,
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
                        // Scoped to the user and chat, in addition to globally available tools
                        tools(ingredientStore.toTools(userId))
                        tools(ChatNamingTools(chatRepository, eventRepository, userId, chatId))
                    },
            )
        agent.run(Unit, chatId.value.toString())
    }
}
