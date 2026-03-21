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

// Agent events

@Serializable
sealed interface UserEvent : Event

@ConsistentCopyVisibility
@Serializable
data class UserMessage private constructor(
    override val text: String?,
    val imageId: FileId?,
) : UserEvent,
    Message {
    init {
        require(text == null || text.isNotBlank()) {
            "Message text must not be blank"
        }
        require(text != null || imageId != null) {
            "Message must contain text or imageId"
        }
    }
}

@Serializable
data object UserWaiting : UserEvent
