package se.gustavkarlsson.chefgpt.routes

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.agent.ChatAgent
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.createEvent
import se.gustavkarlsson.chefgpt.files.AttachmentTextLoader
import se.gustavkarlsson.chefgpt.getChatId
import se.gustavkarlsson.chefgpt.jobs.AgentJobScope
import se.gustavkarlsson.chefgpt.jobs.JobRepository
import se.gustavkarlsson.chefgpt.requireSession

private val logger = LoggerFactory.getLogger("ChatActionsRoute")

fun Route.chatActionsRoute() {
    post("/chats/{chatId}/actions") {
        val userId = call.requireSession().user.id
        val routingContext = this
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
                        val jobRepository = get<JobRepository>()
                        val job = jobRepository.create()
                        val chatAgent = get<ChatAgent>()
                        get<AgentJobScope>().launch {
                            try {
                                with(chatAgent) { routingContext.run(userId, chatId) }
                                jobRepository.succeed(job.id, null)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                logger.error("Chat agent failed", e)
                                jobRepository.fail(
                                    job.id,
                                    ApiError("agent-failed", e.message ?: "Agent failed", userMessage = null),
                                )
                            }
                        }
                        call.respond(HttpStatusCode.Accepted, job)
                    }
                }
            }.onErr { error ->
                call.respond(error.status, error.body)
            }
    }
}
