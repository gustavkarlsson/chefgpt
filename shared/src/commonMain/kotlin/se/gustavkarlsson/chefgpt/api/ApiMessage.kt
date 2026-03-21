package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable

@Serializable
sealed interface ApiMessage

@Serializable
sealed interface ApiAgentMessage : ApiMessage {
    @Serializable
    data class Regular(
        val text: String,
    ) : ApiAgentMessage

    @Serializable
    data class Reasoning(
        val text: String,
    ) : ApiAgentMessage
}

@Serializable
sealed interface ApiUserMessage {
    @Serializable
    data object Waiting : ApiUserMessage

    @Serializable
    data class Regular(
        val text: String?,
        val imageFileId: FileId? = null,
    ) : ApiUserMessage
}
