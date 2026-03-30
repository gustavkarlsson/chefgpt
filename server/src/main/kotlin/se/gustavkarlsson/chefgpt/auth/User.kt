package se.gustavkarlsson.chefgpt.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("user")
data class User(
    val id: UserId,
    val name: String,
)
