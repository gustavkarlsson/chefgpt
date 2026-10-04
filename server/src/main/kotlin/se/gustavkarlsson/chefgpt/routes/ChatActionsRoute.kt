package se.gustavkarlsson.chefgpt.routes

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.agent.chat.ChatAgent
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiAction
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.chats.v1.CHATS_V1_PATH
import se.gustavkarlsson.chefgpt.api.common.UnitSerializer
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.toEvent
import se.gustavkarlsson.chefgpt.files.HtmlLoader
import se.gustavkarlsson.chefgpt.getChatId
import se.gustavkarlsson.chefgpt.jobs.JobRunner
import se.gustavkarlsson.chefgpt.requireSession

fun Route.chatActionsRoute() {
    post("$CHATS_V1_PATH/{chatId}/actions") {
        val userId = call.requireSession().user.id
        call
            .getChatId()
            .onOk { chatId ->
                val eventRepository = get<EventRepository>()
                val action = call.receive<ApiAction>()
                eventRepository.append(chatId, action.toEvent(get<HtmlLoader>()))
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
