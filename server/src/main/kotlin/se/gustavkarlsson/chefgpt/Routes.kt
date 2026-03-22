package se.gustavkarlsson.chefgpt

import ai.koog.ktor.aiAgent
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
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
import io.ktor.server.sse.send
import io.ktor.server.sse.sse
import io.ktor.server.util.getOrFail
import kotlinx.coroutines.flow.takeWhile
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import se.gustavkarlsson.chefgpt.api.End
import se.gustavkarlsson.chefgpt.api.UserEvent
import se.gustavkarlsson.chefgpt.auth.User
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.chats.ChatId
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventFlowManager
import se.gustavkarlsson.chefgpt.images.ImageUploader
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
        route("/chats") {
            // Start a new chat and return the ChatId
            post {
                val chatRepository: ChatRepository by application.dependencies
                val user = call.requireUser()
                val chat = chatRepository.create(user.id)
                call.respond(HttpStatusCode.Created, chat.id.value.toString())
            }
            route("/{chatId}") {
                post("/images") {
                    call.requireValidChatId()
                    val imageUploader: ImageUploader by application.dependencies
                    val contentType = call.request.contentType()
                    val imageUrl = imageUploader.uploadImage(call.receive(), contentType)
                    call.respond(HttpStatusCode.Created, imageUrl.value)
                }

                route("/messages") {
                    // Post a message to the chat and await the response
                    post {
                        val eventFlowManager: EventFlowManager by application.dependencies
                        val chatId = call.requireValidChatId()
                        val userMessage = call.receive<UserEvent>()
                        eventFlowManager.use(chatId) { flow ->
                            val agent =
                                aiAgent<UserEvent, Unit>(
                                    strategy = findRecipeStrategy(flow::emit),
                                    model = AnthropicModels.Haiku_4_5,
                                )
                            agent.run(userMessage, chatId.value.toString())
                        }
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
    // SSE can't use authenticate{} — the connection upgrade runs before the auth pipeline phase.
    // Credentials are validated manually instead.
    route("/chats/{chatId}/messages") {
        // Get the conversation history up to this point (empty if new convo)
        sse(
            serialize = { typeInfo, value ->
                val json: Json by application.dependencies
                val serializer = json.serializersModule.serializer(typeInfo.kotlinType!!)
                json.encodeToString(serializer, value)
            },
        ) {
            val userRepository: UserRepository by application.dependencies
            val credentials = call.request.basicAuthenticationCredentials()
            val user = credentials?.let { userRepository.login(it.name, it.password) }
            if (user == null) {
                // FIXME can't respond with a 401, as the request has already been accepted. Route-scoped plugin?
                return@sse
            }
            val eventFlowManager: EventFlowManager by application.dependencies
            val chatId = call.requireValidChatId(user)
            eventFlowManager.use(chatId) { flow ->
                // TODO chunk based on time
                flow
                    .takeWhile { it !is End }
                    .collect { event ->
                        send(event)
                    }
                send(End)
            }
        }
    }
}

private fun ApplicationCall.requireUser(): User =
    checkNotNull(principal<User>()) {
        "User principal missing. Are you calling this in a non-authenticated endpoint?"
    }

private suspend fun ApplicationCall.requireValidChatId(user: User = requireUser()): ChatId {
    val chatRepository: ChatRepository by application.dependencies
    val chatId = ChatId(parameters.requireUuid("chatId"))
    if (!chatRepository.contains(user.id, chatId)) {
        throw NotFoundException("Chat not found for user")
    }
    return chatId
}

private fun Parameters.requireUuid(name: String): Uuid {
    val value = getOrFail<String>(name)
    return Uuid.parseOrNull(value) ?: throw BadRequestException("Query parameter $name is not a valid UUID")
}
