package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ApiAgentMessageChunk {
    @Serializable
    @SerialName("text")
    data class Text(
        val text: String,
    ) : ApiAgentMessageChunk

    @Serializable
    @SerialName("multiple-choice-question")
    data class MultipleChoiceQuestion(
        val question: String,
        val answers: List<String>,
    ) : ApiAgentMessageChunk
}
