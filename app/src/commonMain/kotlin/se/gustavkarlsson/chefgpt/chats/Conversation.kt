package se.gustavkarlsson.chefgpt.chats

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiAction
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiEvent
import se.gustavkarlsson.chefgpt.api.common.ChatId
import se.gustavkarlsson.chefgpt.jobs.AwaitJobError
import se.gustavkarlsson.chefgpt.sessions.SessionId

interface Conversation {
    val sessionId: SessionId
    val chatId: ChatId

    suspend fun sendAction(action: ApiAction): Result<Unit, AwaitJobError>

    fun events(): Flow<Result<ApiEvent, EventStreamError>>
}

enum class EventStreamError {
    NetworkIo,
    EventHistoryIo,
}
