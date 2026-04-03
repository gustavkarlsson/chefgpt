package se.gustavkarlsson.chefgpt

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.chats.Conversation
import se.gustavkarlsson.chefgpt.chats.EventHistoryStore
import se.gustavkarlsson.chefgpt.sessions.SessionId

class ApiConversationFactory(
    private val client: ChefGptClient,
    private val history: EventHistoryStore,
) : ConversationFactory {
    override fun create(
        sessionId: SessionId,
        chatId: ChatId,
    ): Conversation = ApiConversation(sessionId, chatId, client, history)
}

private class ApiConversation(
    override val sessionId: SessionId,
    override val chatId: ChatId,
    private val client: ChefGptClient,
    private val history: EventHistoryStore,
) : Conversation {
    override suspend fun send(action: ApiAction): Result<Unit, ErrorResponse> =
        client.sendAction(sessionId, chatId, action)

    override fun streamEvents(): Flow<ApiEvent> =
        flow {
            val pastEvents = history.load(chatId)
            for (event in pastEvents) {
                emit(event)
            }
            val lastEventId = pastEvents.lastOrNull()?.id
            val stream =
                client
                    .listenToEvents(sessionId, chatId, lastEventId)
                    .onEach { event ->
                        history.append(chatId, event)
                    }
            emitAll(stream)
        }
}
