package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
sealed interface MessageFromUser {
    @Serializable
    data class Content @OptIn(ExperimentalUuidApi::class) constructor(
        val text: String?,
        val image: Image? = null,
    ) : MessageFromUser

    @Serializable
    data class Image @OptIn(ExperimentalUuidApi::class) constructor(
        val fileName: String,
    )

}
