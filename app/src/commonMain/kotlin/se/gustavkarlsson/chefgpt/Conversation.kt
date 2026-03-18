package se.gustavkarlsson.chefgpt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Conversation : AutoCloseable {
    val state: StateFlow<ConversationState>
    val messageHistory: Flow<Message>

    suspend fun sayToAi(content: MessageContent)
}

enum class Subject {
    User,
    Ai,
}

data class Message(
    val subject: Subject,
    val content: MessageContent,
)

data class MessageContent(
    val reasoning: Boolean, // TODO Make separate message type instead
    val text: String,
    val image: File? = null,
)

enum class ConversationState {
    WaitingForUser,
    WaitingForAi,
    Ended,
}
