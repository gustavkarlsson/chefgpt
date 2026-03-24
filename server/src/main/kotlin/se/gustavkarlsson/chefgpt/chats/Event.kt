package se.gustavkarlsson.chefgpt.chats

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid
import ai.koog.prompt.message.Message as KoogMessage

@Serializable
sealed interface Event {
    val id: Uuid
    val timestamp: Instant

    @Serializable
    data class Message(
        override val id: Uuid,
        val message: KoogMessage,
    ) : Event {
        override val timestamp: Instant
            get() = message.metaInfo.timestamp
    }

    @Serializable
    data class UserJoined(
        override val id: Uuid,
        override val timestamp: Instant,
        val joinId: Uuid,
    ) : Event
}
