package se.gustavkarlsson.chefgpt.routes

import com.github.michaelbull.result.map
import com.github.michaelbull.result.onOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.scope
import se.gustavkarlsson.chefgpt.ResponseData
import se.gustavkarlsson.chefgpt.agent.AiAgent
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.createEvent
import se.gustavkarlsson.chefgpt.getChatId
import se.gustavkarlsson.chefgpt.respond

fun Route.chatActionsRoute() {
    post("/chats/{chatId}/actions") {
        call
            .getChatId()
            .onOk { chatId ->
                val eventRepository = get<EventRepository>()
                val action = call.receive<ApiAction>()
                eventRepository.append(chatId, action.createEvent())
                when (action) {
                    is ApiUserJoinedChat -> {
                        Unit
                    }

                    is ApiUserSendsMessage -> {
                        val aiAgent = call.scope.get<AiAgent>()
                        with(aiAgent) { run(chatId) }
                    }
                }
            }.map {
                ResponseData(HttpStatusCode.NoContent)
            }.respond(call)
    }
}
