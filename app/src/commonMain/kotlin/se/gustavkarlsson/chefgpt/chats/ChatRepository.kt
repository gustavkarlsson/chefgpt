package se.gustavkarlsson.chefgpt.chats

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.ErrorResponse
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.sessions.SessionId

interface ChatRepository {
    suspend fun create(sessionId: SessionId): Result<Chat, ErrorResponse>

    suspend fun getAll(sessionId: SessionId): Flow<Result<List<Chat>, ErrorResponse>>

    suspend fun delete(
        sessionId: SessionId,
        chatId: ChatId,
    ): Result<Unit, ErrorResponse>
}
