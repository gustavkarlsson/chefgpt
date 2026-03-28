package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val type: String,
    val message: String,
    val userMessage: String? = null,
)
