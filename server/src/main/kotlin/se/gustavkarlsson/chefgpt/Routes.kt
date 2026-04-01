package se.gustavkarlsson.chefgpt

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toErrorIfNull
import com.github.michaelbull.result.toResultOr
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basicAuthenticationCredentials
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
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
import org.koin.ktor.ext.inject
import se.gustavkarlsson.chefgpt.agent.AiAgent
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiChat
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.LoginError
import se.gustavkarlsson.chefgpt.auth.RegistrationError
import se.gustavkarlsson.chefgpt.auth.Session
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.chats.Chat
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventRepository
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
                val userRepository: UserRepository by call.inject()
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
                Session(user)
            }.onOk { session ->
                call.sessions.set(session)
            }.map {
                ResponseData(status = HttpStatusCode.NoContent)
            }.respond(call)
    }
    post("/login") {
        call
            .getCredentials()
            .flatMap { credentials ->
                val userRepository: UserRepository by call.inject()
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
                Session(user)
            }.onOk { session ->
                call.sessions.set(session)
            }.map {
                ResponseData(status = HttpStatusCode.NoContent)
            }.respond(call)
    }
    authenticate {
        // Upload an image and return the URL
        post("/images") {
            val imageUploader: ImageUploader by call.inject()
            val contentType = call.request.contentType()
            val imageUrl = imageUploader.uploadImage(call.receive(), contentType)
            call.respond(HttpStatusCode.Created, imageUrl.value)
        }
        route("/chats") {
            // List all chats for the authenticated user
            get {
                val chatRepository: ChatRepository by call.inject()
                val session = call.requireSession()
                val chats = chatRepository.getAll(session.user.id)
                call.respond(HttpStatusCode.OK, chats.map { it.toApi() })
            }
            // Start a new chat and return the ApiChat
            post {
                val chatRepository: ChatRepository by call.inject()
                val session = call.requireSession()
                val chat = chatRepository.create(session.user.id)
                call.respond(HttpStatusCode.Created, chat.toApi())
            }
            route("/{chatId}") {
                // Delete a chat
                delete {
                    val session = call.requireSession()
                    val rawChatId = call.parameters.getOrFail("chatId")
                    val chatId = ChatId.parseOrNull(rawChatId)
                    if (chatId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError("invalid-chat-id", "Invalid chat ID"),
                        )
                        return@delete
                    }
                    val chatRepository: ChatRepository by call.inject()
                    val deleted = chatRepository.delete(session.user.id, chatId)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiError("chat-not-found", "Chat not found"),
                        )
                    }
                }
                // Get a flow of chat events
                val json: Json by inject()
                sse(
                    "/events",
                    serialize = { typeInfo, value ->
                        val serializer = json.serializersModule.serializer(typeInfo.kotlinType!!)
                        json.encodeToString(serializer, value)
                    },
                ) {
                    heartbeat {
                        period = 1.seconds
                        event = ServerSentEvent("heartbeat")
                    }
                    call.getChatId().onOk { chatId ->
                        val eventRepository: EventRepository by call.inject()
                        eventRepository
                            .flow(chatId)
                            .mapNotNull { it.toApiOrNull() }
                            .collect { apiEvent: ApiEvent ->
                                // TODO Batch events to improve efficiency. Maybe make a Batch event?
                                send(apiEvent)
                            }
                    }
                }
                // Send an event, some of which may be processed by an LLM
                post("/actions") {
                    val session = call.requireSession()
                    call
                        .getChatId()
                        .onOk { chatId ->
                            val eventRepository: EventRepository by call.inject()
                            val action = call.receive<ApiAction>()
                            eventRepository.append(chatId, action.createEvent())
                            when (action) {
                                is ApiUserJoinedChat -> {
                                    Unit
                                }

                                is ApiUserSendsMessage -> {
                                    val aiAgent: AiAgent by call.inject()
                                    with(aiAgent) { run(session.user.id, chatId) }
                                }
                            }
                        }.map {
                            ResponseData(HttpStatusCode.NoContent)
                        }.respond(call)
                }
            }
        }
    }
}

private fun ApplicationCall.getCredentials(): Result<UserPasswordCredential, ResponseData<ApiError?>> =
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

fun ApplicationCall.requireSession(): Session =
    checkNotNull(principal<Session>()) {
        "User principal missing. Are we calling this in a non-authenticated endpoint?"
    }

private suspend fun ApplicationCall.getChatId(): Result<ChatId, ResponseData<ApiError>> {
    val rawChatId = parameters.getOrFail("chatId")
    return ChatId
        .parseOrNull(rawChatId)
        .toResultOr {
            ResponseData(
                status = HttpStatusCode.BadRequest,
                body = ApiError("invalid-chat-id", "Invalid chat ID"),
            )
        }.flatMap { chatId ->
            val chatRepository: ChatRepository by inject()
            val session = requireSession()
            chatRepository[session.user.id, chatId].toResultOr {
                ResponseData(
                    status = HttpStatusCode.NotFound,
                    body = ApiError("chat-not-found", "Chat not found"),
                )
            }
        }.map { chat ->
            chat.id
        }
}

private fun Chat.toApi(): ApiChat = ApiChat(id, createdAt)
