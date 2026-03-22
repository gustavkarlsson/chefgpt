package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * An event represents anything that can occur in the communication between client and server
 */
@Serializable
sealed interface Event

/**
 * An action is an event that's visible in the chat in some way
 */
@Serializable
sealed interface Action : Event {
    val text: String?
}

@Serializable
sealed interface AgentEvent : Event

@Serializable
sealed interface AgentAction :
    Action,
    AgentEvent

@Serializable
data class AgentMessage(
    override val text: String,
) : AgentAction

@Serializable
data class AgentReasoning(
    override val text: String,
) : AgentAction

@Serializable
data object AgentToolCall : AgentAction {
    override val text: Nothing?
        get() = null
}

@Serializable
sealed interface UserEvent : Event

@Serializable
sealed interface UserAction :
    Action,
    UserEvent

@Serializable
data class UserMessage(
    override val text: String?,
    val imageUrl: ImageUrl? = null,
) : UserAction {
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
data class UserJoinedChat(
    val id: Uuid,
) : UserEvent
