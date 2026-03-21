package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable

@Serializable
sealed interface Event {
    @Serializable
    data class Message(
        val message: se.gustavkarlsson.chefgpt.api.Message,
    ) : Event

    @Serializable
    data object End : Event
}
