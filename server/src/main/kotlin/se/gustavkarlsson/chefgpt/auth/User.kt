package se.gustavkarlsson.chefgpt.auth

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: UserId,
    val name: String,
)
