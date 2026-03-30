package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@SerialName("api-chat")
data class ApiChat(
    val id: ChatId,
    val createdAt: Instant,
)
