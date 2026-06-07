package se.gustavkarlsson.chefgpt.chats

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.sessions.SessionId

interface Conversation {
    val sessionId: SessionId
    val chatId: ChatId

    suspend fun sendAction(action: ApiAction): Result<Unit, ClientError>

    fun events(): Flow<Result<ApiEvent, EventStreamError>>
}

enum class EventStreamError {
    NetworkIo,
    EventHistoryIo,
}
