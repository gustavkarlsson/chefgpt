package se.gustavkarlsson.chefgpt.chats

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.ErrorResponse
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.sessions.SessionId

interface Conversation {
    val sessionId: SessionId
    val chatId: ChatId

    suspend fun send(action: ApiAction): Result<Unit, ErrorResponse>

    fun streamEvents(): Flow<ApiEvent> // TODO Result?
}
