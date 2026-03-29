package se.gustavkarlsson.chefgpt.chats

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.api.JoinId
import kotlin.time.Instant
import ai.koog.prompt.message.Message as KoogMessage

@Serializable
sealed interface Event {
    val id: EventId
    val timestamp: Instant

    @Serializable
    data class Message(
        override val id: EventId,
        val message: KoogMessage,
    ) : Event {
        override val timestamp: Instant
            get() = message.metaInfo.timestamp
    }

    @Serializable
    data class UserJoined(
        override val id: EventId,
        override val timestamp: Instant,
        val joinId: JoinId,
    ) : Event
}
