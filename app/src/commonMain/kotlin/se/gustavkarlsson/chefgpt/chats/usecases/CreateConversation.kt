package se.gustavkarlsson.chefgpt.chats.usecases

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.UnitSerializer
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.chats.Conversation
import se.gustavkarlsson.chefgpt.chats.EventHistoryStore
import se.gustavkarlsson.chefgpt.chats.EventStreamError
import se.gustavkarlsson.chefgpt.jobs.AwaitJobError
import se.gustavkarlsson.chefgpt.jobs.usecases.AwaitJob
import se.gustavkarlsson.chefgpt.sessions.SessionId

private val log = Logger.withTag("${HttpCreateConversation::class.simpleName}")

fun interface CreateConversation {
    operator fun invoke(
        sessionId: SessionId,
        chatId: ChatId,
    ): Conversation
}

class HttpCreateConversation(
    private val client: ChefGptClient,
    private val history: EventHistoryStore,
    private val awaitJob: AwaitJob,
) : CreateConversation {
    override operator fun invoke(
        sessionId: SessionId,
        chatId: ChatId,
    ): Conversation = ApiConversation(sessionId, chatId, client, history, awaitJob)
}

private class ApiConversation(
    override val sessionId: SessionId,
    override val chatId: ChatId,
    private val client: ChefGptClient,
    private val history: EventHistoryStore,
    private val awaitJob: AwaitJob,
) : Conversation {
    override suspend fun sendAction(action: ApiAction): Result<Unit, AwaitJobError> =
        when (action) {
            is ApiUserJoinedChat -> {
                client
                    .joinChat(sessionId, chatId, action.joinId)
                    .mapError { AwaitJobError.RequestFailed(it) }
            }

            is ApiUserSendsMessage -> {
                awaitJob(sessionId, UnitSerializer) { client.sendAction(sessionId, chatId, action) }
                    .map { Unit }
            }
        }

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
