package se.gustavkarlsson.chefgpt

import java.nio.file.Path
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface UserSideConversation : AutoCloseable {
    val state: StateFlow<ConversationState>
    val messageHistory: Flow<Message>
    suspend fun sayToAi(content: MessageContent)
}

interface AiSideConversation : AutoCloseable {
    suspend fun sayToUser(content: MessageContent)
    suspend fun listenToUser(): MessageContent
    suspend fun askUser(content: MessageContent): MessageContent {
        sayToUser(content)
        return listenToUser()
    }
}

enum class Subject {
    User, Ai,
}

data class Message(val subject: Subject, val content: MessageContent)

data class MessageContent(val text: String, val image: Path? = null)

enum class ConversationState {
    WaitingForUser, WaitingForAi, Ended,
}

class ConversationService : AiSideConversation, UserSideConversation {
    private val _state = MutableStateFlow(ConversationState.WaitingForAi)
    override val state: StateFlow<ConversationState> = _state.asStateFlow()

    private val messageChannel = Channel<Message>(capacity = Int.MAX_VALUE)
    override val messageHistory: Flow<Message> = messageChannel.consumeAsFlow()

    private val messagesToAi = MutableSharedFlow<MessageContent>()

    private val aiMutex = Mutex()
    private val userMutex = Mutex()

    override suspend fun sayToAi(content: MessageContent) {
        userMutex.withLock {
            checkNotEnded()
            _state.value = ConversationState.WaitingForAi
            messageChannel.send(Message(Subject.User, content))
            messagesToAi.emit(content)
        }
    }

    override suspend fun sayToUser(content: MessageContent) {
        aiMutex.withLock {
            checkNotEnded()
            messageChannel.send(Message(Subject.Ai, content))
        }
    }

    override suspend fun listenToUser(): MessageContent {
        return aiMutex.withLock {
            coroutineScope {
                checkNotEnded()
                // First start listening
                val message = async { messagesToAi.first() }
                // Then toggle the state to accept messages
                _state.value = ConversationState.WaitingForUser
                // Finally await the message
                message.await()
            }
        }
    }

    private fun checkNotEnded() {
        check(_state.value != ConversationState.Ended) { "Conversation has ended" }
    }

    override fun close() {
        _state.value = ConversationState.Ended
        messageChannel.close()
    }
}
