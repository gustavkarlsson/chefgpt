package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface StreamChats {
    suspend operator fun invoke(sessionId: SessionId): Flow<List<Chat>>
}

class HttpStreamChats(
    private val repository: ChatRepository,
) : StreamChats {
    override suspend fun invoke(sessionId: SessionId): Flow<List<Chat>> = repository.stream(sessionId)
}
