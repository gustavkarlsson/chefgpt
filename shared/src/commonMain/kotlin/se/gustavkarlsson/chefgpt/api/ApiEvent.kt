package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable

@Serializable
sealed interface ApiEvent {
    @Serializable
    data class Message(
        val message: ApiMessage,
    ) : ApiEvent

    @Serializable
    data object End : ApiEvent
}
