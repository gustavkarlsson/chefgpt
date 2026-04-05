package se.gustavkarlsson.chefgpt.chats

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ErrorResponse
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.sessions.SessionId

private val log = Logger.withTag("${ApiConversationFactory::class.simpleName}")

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
    override suspend fun sendAction(action: ApiAction): Result<Unit, ErrorResponse> =
        client.sendAction(sessionId, chatId, action)

    override fun events(): Flow<Result<ApiEvent, EventStreamError>> =
        flow {
            history
                .load(chatId)
                .onErr {
                    emit(Err(EventStreamError.EventHistoryIo))
                }.onOk { pastEvents ->
                    for (event in pastEvents) {
                        emit(Ok(event))
                    }
                    val lastEventId = pastEvents.lastOrNull()?.id
                    emitAll(streamEventResults(lastEventId))
                }
        }

    private fun streamEventResults(lastEventId: EventId?): Flow<Result<ApiEvent, EventStreamError>> =
        flow {
            try {
                // TODO After fixing error handling in listenToEvents, we can remove the try-catch
                client
                    .listenToEvents(sessionId, chatId, lastEventId)
                    .transformWhile { event ->
                        // This lambda emits to the transform collector
                        history
                            .append(chatId, event)
                            .onErr { emit(Err(EventStreamError.EventHistoryIo)) }
                            .onOk { emit(Ok(event)) }
                            .isOk // Cancel streaming if error
                    }.collect { eventResult -> emit(eventResult) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to stream events from API" }
                emit(Err(EventStreamError.NetworkIo))
            }
        }
}
