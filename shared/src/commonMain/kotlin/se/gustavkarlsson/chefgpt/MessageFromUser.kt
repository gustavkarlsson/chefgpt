package se.gustavkarlsson.chefgpt

import kotlinx.serialization.Serializable

@Serializable
sealed interface MessageFromUser {
    @Serializable
    data class Content(
        val text: String,
    ) : MessageFromUser

    @Serializable
    data object End : MessageFromUser
}
