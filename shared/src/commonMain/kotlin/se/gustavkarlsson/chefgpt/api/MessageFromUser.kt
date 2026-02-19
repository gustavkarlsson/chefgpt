package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable

@Serializable
data class MessageFromUser(
    val text: String?,
    val image: Image? = null,
)

@Serializable
data class Image(
    val fileName: String,
)
