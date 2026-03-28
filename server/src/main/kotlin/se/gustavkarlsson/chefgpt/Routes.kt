package se.gustavkarlsson.chefgpt

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toErrorIfNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.UserPasswordCredential
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
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
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
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.SessionId
import se.gustavkarlsson.chefgpt.auth.LoginError
import se.gustavkarlsson.chefgpt.auth.RegistrationError
import se.gustavkarlsson.chefgpt.auth.Session
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.chats.Chat
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.createEvent
import se.gustavkarlsson.chefgpt.chats.toApiOrNull
import se.gustavkarlsson.chefgpt.images.ImageUploader
import kotlin.time.Duration.Companion.seconds

// TODO set timeouts
fun Routing.routes() {
    post("/register") {
        call
            .getCredentials()
            .flatMap { credentials ->
                val userRepository: UserRepository by application.dependencies
                userRepository.register(credentials.name, credentials.password).mapError { registrationError ->
                    when (registrationError) {
                        is RegistrationError.InvalidUserName -> {
                            ResponseData(
                                status = HttpStatusCode.BadRequest,
                                body = ApiError("invalid-username", registrationError.message),
                            )
                        }

                        is RegistrationError.InvalidPassword -> {
                            ResponseData(
                                status = HttpStatusCode.BadRequest,
                                body = ApiError("invalid-password", registrationError.message),
                            )
                        }

                        RegistrationError.UsernameTaken -> {
                            ResponseData(HttpStatusCode.Conflict)
                        }
                    }
                }
            }.map { user ->
                val sessionId = SessionId.random()
                call.sessions.set(Session(sessionId, user))
                ResponseData(HttpStatusCode.Created, sessionId.toString())
            }.respond(call)
    }
    post("/login") {
        call
            .getCredentials()
            .flatMap { credentials ->
                val userRepository: UserRepository by application.dependencies
                userRepository.login(credentials.name, credentials.password).mapError { loginError ->
                    when (loginError) {
                        LoginError.WrongCredentials -> {
                            ResponseData(
                                status = HttpStatusCode.Unauthorized,
                                body = ApiError("wrong-credentials", "Wrong credentials"),
                            )
                        }
                    }
                }
            }.map { user ->
                val sessionId = SessionId.random()
                call.sessions.set(Session(sessionId, user))
                ResponseData(HttpStatusCode.OK, sessionId.toString())
            }.respond(call)
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
                val session = call.requireSession()
                val chat = chatRepository.create(session.user.id)
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
                    val session = call.requireSession()
                    val chat = call.requireChat()
                    val action = call.receive<ApiAction>()
                    chat.append(action.createEvent())
                    when (action) {
                        is ApiUserJoinedChat -> {
                            call.respond(HttpStatusCode.OK)
                        }

                        is ApiUserSendsMessage -> {
                            runAgent(session.user.id, chat.id)
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.getCredentials(): Result<UserPasswordCredential, ResponseData<ApiError>> =
    runCatching { request.basicAuthenticationCredentials() }
        .mapError { throwable ->
            when (throwable) {
                is BadRequestException -> {
                    ResponseData(
                        status = HttpStatusCode.BadRequest,
                        body = ApiError("invalid-credentials", "Invalid credentials"),
                    )
                }

                else -> {
                    ResponseData(HttpStatusCode.InternalServerError)
                }
            }
        }.toErrorIfNull {
            ResponseData(
                status = HttpStatusCode.BadRequest,
                body = ApiError("missing-credentials", "Missing credentials"),
            )
        }

private fun ApplicationCall.requireSession(): Session =
    checkNotNull(principal<Session>()) {
        "User principal missing. Are we calling this in a non-authenticated endpoint?"
    }

// FIXME return result instead, as this might be a user error
private suspend fun ApplicationCall.requireChat(): Chat {
    val chatRepository: ChatRepository by application.dependencies
    val session = requireSession()
    val chatId = requireChatId()
    return chatRepository[session.user.id, chatId] ?: throw NotFoundException("Chat not found for user")
}

private fun ApplicationCall.requireChatId(): ChatId = ChatId.parse(parameters.getOrFail("chatId"))
