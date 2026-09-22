package se.gustavkarlsson.chefgpt.chats

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface CreateChat {
    suspend operator fun invoke(sessionId: SessionId): Result<Chat, ClientError>
}

class HttpCreateChat(
    private val repository: ChatRepository,
) : CreateChat {
    override suspend fun invoke(sessionId: SessionId): Result<Chat, ClientError> = repository.create(sessionId)
}
