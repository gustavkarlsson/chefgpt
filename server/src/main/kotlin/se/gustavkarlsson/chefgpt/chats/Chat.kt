package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.common.ChatId
import kotlin.time.Instant

data class Chat(
    val id: ChatId,
    val createdAt: Instant,
    val name: String?,
)
