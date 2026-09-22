package se.gustavkarlsson.chefgpt.chats.usecases

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface DeleteChat {
    suspend operator fun invoke(
        sessionId: SessionId,
        chatId: ChatId,
    ): Result<Unit, ClientError>
}

class HttpDeleteChat(
    private val repository: ChatRepository,
) : DeleteChat {
    override suspend fun invoke(
        sessionId: SessionId,
        chatId: ChatId,
    ): Result<Unit, ClientError> = repository.delete(sessionId, chatId)
}
