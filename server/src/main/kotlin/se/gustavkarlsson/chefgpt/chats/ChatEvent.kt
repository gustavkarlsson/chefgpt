package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserMessage

sealed interface ChatEvent {
    data class NewMessage(
        val message: Message,
    ) : ChatEvent

    data object End : ChatEvent
}

fun ChatEvent.toApi(): ApiEvent =
    when (this) {
        is ChatEvent.NewMessage -> {
            val apiMessage =
                when (message) {
                    is AgentMessage.Reasoning -> ApiAgentMessage.Reasoning(message.text)
                    is AgentMessage.Regular -> ApiAgentMessage.Regular(message.text)
                    is UserMessage -> ApiUserMessage.Regular(message.text, message.imageId)
                }
            ApiEvent.Message(apiMessage)
        }

        ChatEvent.End -> {
            ApiEvent.End
        }
    }
