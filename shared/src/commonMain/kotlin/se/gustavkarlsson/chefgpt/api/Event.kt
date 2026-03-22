package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable

@Serializable
sealed interface Event

@Serializable
sealed interface Message : Event {
    val text: String?
}

// Agent events

@Serializable
sealed interface AgentEvent : Event

@Serializable
sealed interface AgentMessage :
    AgentEvent,
    Message {
    @Serializable
    data class Regular(
        override val text: String,
    ) : AgentMessage

    @Serializable
    data class Reasoning(
        override val text: String,
    ) : AgentMessage
}

@Serializable
data object End : AgentEvent

@Serializable
data object ToolCall : AgentEvent

// Agent events

@Serializable
sealed interface UserEvent : Event

@Serializable
data class UserMessage(
    override val text: String?,
    val imageUrl: ImageUrl? = null,
) : UserEvent,
    Message {
    init {
        require(text == null || text.isNotBlank()) {
            "Message text must not be blank"
        }
        require(text != null || imageUrl != null) {
            "Message must contain text or imageUrl"
        }
    }
}

@Serializable
data object UserWaiting : UserEvent
