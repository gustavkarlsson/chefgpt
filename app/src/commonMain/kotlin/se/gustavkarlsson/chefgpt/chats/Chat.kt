package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.ChatId
import kotlin.time.Instant

data class Chat(
    val id: ChatId,
    val createdAt: Instant,
    val name: String? = null,
)

// Falls back to a timestamp-based title when the chat hasn't been named yet.
val Chat.displayName: String
    get() = name ?: "Chat from ${createdAt.toString().replace("T", " ").take(19)}"
