package se.gustavkarlsson.chefgpt.agent

import ai.koog.ktor.aiAgent
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import io.ktor.server.routing.RoutingContext
import se.gustavkarlsson.chefgpt.api.UserMessage
import se.gustavkarlsson.chefgpt.chats.ChatId
import se.gustavkarlsson.chefgpt.chats.EventFlowManager

suspend fun RoutingContext.runAgent(
    chatId: ChatId,
    userMessage: UserMessage,
    eventFlowManager: EventFlowManager,
) {
    eventFlowManager.use(chatId) { flow ->
        val agent =
            aiAgent(
                strategy = findRecipeStrategy(flow::emit),
                model = AnthropicModels.Haiku_4_5,
            )
        agent.run(userMessage, chatId.value.toString())
    }
}
