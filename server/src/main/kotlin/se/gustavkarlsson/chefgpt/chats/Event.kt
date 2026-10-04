package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.common.EventId
import se.gustavkarlsson.chefgpt.api.common.JoinId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import kotlin.time.Instant

sealed interface Event {
    val id: EventId
    val timestamp: Instant

    data class Message(
        override val id: EventId,
        val message: ChatMessage,
        // The files the user attached, kept alongside the message because a Koog attachment loses
        // its url once the content has been inlined (which text attachments require).
        val attachments: List<UploadedFile>,
    ) : Event {
        override val timestamp: Instant
            get() = message.timestamp
    }

    data class UserJoined(
        override val id: EventId,
        override val timestamp: Instant,
        val joinId: JoinId,
    ) : Event

    data class ChatNamed(
        override val id: EventId,
        override val timestamp: Instant,
        val name: String,
    ) : Event
}
