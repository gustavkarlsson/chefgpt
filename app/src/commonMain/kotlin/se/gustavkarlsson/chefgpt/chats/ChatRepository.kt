package se.gustavkarlsson.chefgpt.chats

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.sessions.SessionId

interface ChatRepository {
    suspend fun create(sessionId: SessionId): Result<Chat, ClientError>

    suspend fun stream(sessionId: SessionId): Flow<List<Chat>>

    suspend fun delete(
        sessionId: SessionId,
        chatId: ChatId,
    ): Result<Unit, ClientError>
}
