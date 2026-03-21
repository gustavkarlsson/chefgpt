package se.gustavkarlsson.chefgpt

sealed interface AgentMessage {
    val text: String

    data class Regular(
        override val text: String,
    ) : AgentMessage

    data class Reasoning(
        override val text: String,
    ) : AgentMessage
}
