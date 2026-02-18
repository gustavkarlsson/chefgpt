package se.gustavkarlsson.chefgpt

import kotlinx.serialization.Serializable

@Serializable
sealed interface MessageFromAi {
    @Serializable
    data class Content(val text: String) : MessageFromAi

    @Serializable
    data class End(val text: String) : MessageFromAi
}
