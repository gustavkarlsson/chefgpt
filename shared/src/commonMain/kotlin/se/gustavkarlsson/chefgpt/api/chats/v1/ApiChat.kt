package se.gustavkarlsson.chefgpt.api.chats.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.common.ChatId
import kotlin.time.Instant

@Serializable
@SerialName("api-chat")
data class ApiChat(
    val id: ChatId,
    val createdAt: Instant,
    val name: String?,
)
