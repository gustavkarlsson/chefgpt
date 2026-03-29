package se.gustavkarlsson.chefgpt.auth

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val user: User,
)
