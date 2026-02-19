package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data class MessageFromUser @OptIn(ExperimentalUuidApi::class) constructor(
    val text: String?,
    val image: Image? = null,
)

@Serializable
data class Image @OptIn(ExperimentalUuidApi::class) constructor(
    val fileName: String,
)
