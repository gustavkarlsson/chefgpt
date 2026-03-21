package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.ApiMessage
import se.gustavkarlsson.chefgpt.api.ApiUserMessage
import se.gustavkarlsson.chefgpt.api.FileId
import ai.koog.prompt.message.Message as KoogMessage

sealed interface Message

data class UserMessage(
    val text: String?,
    val imageId: FileId?,
) : Message

sealed interface AgentMessage : Message {
    val text: String

    data class Regular(
        override val text: String,
    ) : AgentMessage

    data class Reasoning(
        override val text: String,
    ) : AgentMessage
}

fun KoogMessage.toDomain(): Message {
    TODO("Convert koog message to domain message")
}

fun Message.toApi(): ApiMessage =
    when (this) {
        is AgentMessage.Reasoning -> ApiAgentMessage.Reasoning(text)
        is AgentMessage.Regular -> ApiAgentMessage.Regular(text)
        is UserMessage -> ApiUserMessage.Regular(text, imageId)
    }
