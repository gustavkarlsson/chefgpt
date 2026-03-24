package se.gustavkarlsson.chefgpt.chats

import kotlinx.serialization.Serializable
import ai.koog.prompt.message.Message as KoogMessage

@Serializable
sealed interface Event {
    @Serializable
    data class Message(
        val message: KoogMessage,
    ) : Event
}
