package se.gustavkarlsson.chefgpt.routes

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.toResultOr
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.sse.send
import kotlinx.coroutines.flow.mapNotNull
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.ResponseData
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.toApiOrNull
import se.gustavkarlsson.chefgpt.getChatId
import se.gustavkarlsson.chefgpt.util.sse

// TODO Add tests (Not snapshot test, as they are not possible)
fun Route.streamChatEventsRoute() {
    sse("/chats/{chatId}/events") {
        val eventRepository = get<EventRepository>()
        call
            .getChatId()
            .flatMap { chatId ->
                val lastEventId = call.request.queryParameters["lastEventId"]
                if (lastEventId != null) {
                    EventId
                        .parseOrNull(lastEventId)
                        .toResultOr {
                            ResponseData(
                                status = HttpStatusCode.BadRequest,
                                body =
                                    ApiError(
                                        "invalid-event-id",
                                        "Query parameter lastEventId=$lastEventId is not valid",
                                    ),
                            )
                        }.map { chatId to it }
                } else {
                    Ok(chatId to null)
                }
            }.onOk { (chatId, lastEventId) ->
                eventRepository
                    .flow(chatId, last = lastEventId)
                    .mapNotNull { it.toApiOrNull() }
                    .collect { apiEvent: ApiEvent ->
                        send(apiEvent, "event")
                    }
            }
    }
}
