package se.gustavkarlsson.chefgpt

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basicAuthenticationCredentials
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.send
import io.ktor.server.sse.sse
import io.ktor.server.util.getOrFail
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import se.gustavkarlsson.chefgpt.agent.runAgent
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.User
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.chats.Chat
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.toApiOrNull
import se.gustavkarlsson.chefgpt.images.ImageUploader
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

// TODO set timeouts
fun Routing.routes() {
    post("/register") {
        val userRepository: UserRepository by application.dependencies
        val credentials =
            call.request.basicAuthenticationCredentials() ?: throw BadRequestException("Invalid Credentials")
        val user = userRepository.register(credentials.name, credentials.password)
        if (user != null) {
            call.respond(HttpStatusCode.Created)
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
    authenticate {
        // Upload an image and return the URL
        post("/images") {
            val imageUploader: ImageUploader by application.dependencies
            val contentType = call.request.contentType()
            val imageUrl = imageUploader.uploadImage(call.receive(), contentType)
            call.respond(HttpStatusCode.Created, imageUrl.value)
        }
        route("/chats") {
            // Start a new chat and return the ChatId
            post {
                val chatRepository: ChatRepository by application.dependencies
                val user = call.requireUser()
                val chat = chatRepository.create(user.id)
                call.respond(HttpStatusCode.Created, chat.id.value.toString())
            }
            route("/{chatId}") {
                // Get a flow of chat events
                sse(
                    "/events",
                    serialize = { typeInfo, value ->
                        val json: Json by application.dependencies
                        val serializer = json.serializersModule.serializer(typeInfo.kotlinType!!)
                        json.encodeToString(serializer, value)
                    },
                ) {
                    heartbeat {
                        period = 1.seconds
                        event = ServerSentEvent("heartbeat")
                    }
                    val chat = call.requireChat()
                    chat.events().mapNotNull { it.toApiOrNull() }.collect { apiEvent: ApiEvent ->
                        // TODO Batch events to improve efficiency. Maybe make a Batch event?
                        send(apiEvent)
                    }
                }
                // Send an event, some of which may be processed by an LLM
                post("/actions") {
                    val chat = call.requireChat()
                    when (val action = call.receive<ApiAction>()) {
                        is ApiUserJoinedChat -> {
                            // Register that someone joined the chat.
                            // The user should start listening to the events before sending this.
                            // They can then observe this value in the event stream to tell when they have "caught up".
                            val event = Event.UserJoined(Uuid.random(), Clock.System.now(), action.joinId)
                            chat.append(event)
                            call.respond(HttpStatusCode.OK)
                        }

                        is ApiUserSendsMessage -> {
                            call.respond(HttpStatusCode.OK)
                            // Will be converted to events after it
                            runAgent(chat.id, action)
                        }
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.requireUser(): User =
    checkNotNull(principal<User>()) {
        "User principal missing. Are you calling this in a non-authenticated endpoint?"
    }

private suspend fun ApplicationCall.requireChat(): Chat {
    val chatRepository: ChatRepository by application.dependencies
    val user = requireUser()
    val chatId = requireChatId()
    return chatRepository[user.id, chatId] ?: throw NotFoundException("Chat not found for user")
}

private fun ApplicationCall.requireChatId(): ChatId = ChatId.parse(parameters.getOrFail("chatId"))
