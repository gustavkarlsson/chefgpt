package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable

@Serializable
sealed interface MessageFromAi {
    @Serializable
    data class Regular(
        val text: String,
    ) : MessageFromAi

    @Serializable
    data class Reasoning(
        val text: String,
    ) : MessageFromAi
}
