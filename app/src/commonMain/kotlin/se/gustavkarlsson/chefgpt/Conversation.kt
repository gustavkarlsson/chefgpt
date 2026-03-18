package se.gustavkarlsson.chefgpt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Conversation : AutoCloseable {
    val state: StateFlow<ConversationState>
    val messageHistory: Flow<Message>

    suspend fun sayToAi(message: UserMessage)
}

sealed interface Message {
    val text: String
    val image: File?
}

sealed interface AiMessage : Message {
    data class Regular(
        override val text: String,
    ) : AiMessage {
        override val image: Nothing?
            get() = null
    }

    data class Reasoning(
        override val text: String,
    ) : AiMessage {
        override val image: Nothing?
            get() = null
    }
}

data class UserMessage(
    override val text: String,
    override val image: File? = null,
) : Message

enum class ConversationState {
    WaitingForUser,
    WaitingForAi,
    Ended,
}
