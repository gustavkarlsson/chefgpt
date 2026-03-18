package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable

@Serializable
data class MessageFromUser(
    val text: String?,
    val imageRef: ImageRef? = null,
)

@Serializable
data class ImageRef(
    val fileName: String,
)
