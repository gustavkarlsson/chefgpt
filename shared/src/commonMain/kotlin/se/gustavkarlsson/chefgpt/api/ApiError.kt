package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-error")
data class ApiError(
    val type: String,
    val message: String,
    val userMessage: String? = null,
)
