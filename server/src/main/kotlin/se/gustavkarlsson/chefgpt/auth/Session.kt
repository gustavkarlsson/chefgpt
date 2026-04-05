package se.gustavkarlsson.chefgpt.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("session")
data class Session(
    val user: User,
)
