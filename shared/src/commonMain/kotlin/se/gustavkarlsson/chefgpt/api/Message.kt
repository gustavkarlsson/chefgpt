package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable

@Serializable
sealed interface Message

@Serializable
sealed interface AgentMessage : Message {
    @Serializable
    data class Regular(
        val text: String,
    ) : AgentMessage

    @Serializable
    data class Reasoning(
        val text: String,
    ) : AgentMessage
}

@Serializable
sealed interface UserMessage : Message {
    @Serializable
    data object Waiting : UserMessage

    @Serializable
    data class Regular(
        val text: String?,
        val imageId: FileId? = null,
    ) : UserMessage
}
