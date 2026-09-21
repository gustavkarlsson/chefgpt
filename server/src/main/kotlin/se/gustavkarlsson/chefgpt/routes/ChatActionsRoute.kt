package se.gustavkarlsson.chefgpt.routes

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.UnitSerializer
import se.gustavkarlsson.chefgpt.agent.chat.ChatAgent
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.createEvent
import se.gustavkarlsson.chefgpt.files.AttachmentTextLoader
import se.gustavkarlsson.chefgpt.getChatId
import se.gustavkarlsson.chefgpt.jobs.JobRunner
import se.gustavkarlsson.chefgpt.requireSession

fun Route.chatActionsRoute() {
    post("/chats/{chatId}/actions") {
        val userId = call.requireSession().user.id
        call
            .getChatId()
            .onOk { chatId ->
                val eventRepository = get<EventRepository>()
                val action = call.receive<ApiAction>()
                eventRepository.append(chatId, action.createEvent(get<AttachmentTextLoader>()))
                when (action) {
                    is ApiUserJoinedChat -> {
                        call.respond(HttpStatusCode.NoContent)
                    }

                    is ApiUserSendsMessage -> {
                        val chatAgent = get<ChatAgent>()
                        val job =
                            get<JobRunner>().run("Chat agent", UnitSerializer) {
                                chatAgent.run(userId, chatId)
                            }
                        call.respond(HttpStatusCode.Accepted, job)
                    }
                }
            }.onErr { error ->
                call.respond(error.status, error.body)
            }
    }
}
