package se.gustavkarlsson.chefgpt.auth

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.SessionId

@Serializable
data class UserSession(
    val sessionId: SessionId,
    val user: User,
)
