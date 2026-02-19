package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable

@Serializable
sealed interface MessageFromAi {
    @Serializable
    data class Content(
        val text: String,
    ) : MessageFromAi

}
