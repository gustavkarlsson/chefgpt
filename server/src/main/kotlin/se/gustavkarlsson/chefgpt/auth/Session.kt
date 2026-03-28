package se.gustavkarlsson.chefgpt.auth

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.SessionId

@Serializable
data class Session(
    val sessionId: SessionId,
    val user: User,
)
