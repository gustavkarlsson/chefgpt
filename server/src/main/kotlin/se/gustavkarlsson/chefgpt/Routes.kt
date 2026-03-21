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
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Routing
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sse.send
import io.ktor.server.sse.sse
import io.ktor.server.util.getOrFail
import kotlinx.coroutines.flow.takeWhile
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import se.gustavkarlsson.chefgpt.api.Event
import se.gustavkarlsson.chefgpt.api.FileId
import se.gustavkarlsson.chefgpt.api.UserMessage
import se.gustavkarlsson.chefgpt.auth.User
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.chats.ChatId
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventFlowManager
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
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
                // Upload an image to the chat and return the FileId
                route("/images") {
                    post {
                        val imageStore: ImageStore by application.dependencies
                        val chatId = call.requireValidChatId()
                        val fileId = imageStore.writeFile(chatId, call.receiveChannel())
                        call.respond(HttpStatusCode.Created, fileId.value.toString())
                    }
                    get("/{fileId}") {
                        val imageStore: ImageStore by application.dependencies
                        val chatId = call.requireValidChatId()
                        val fileIdString = checkNotNull(call.pathParameters["fileId"])
                        val fileId = FileId.parseOrNull(fileIdString) ?: throw NotFoundException()
                        val file = imageStore.getFile(chatId, fileId) ?: throw NotFoundException()
                        call.respondFile(file.toFile())
                    }
                }

                route("/messages") {
                    // Get the conversation history up to this point (empty if new convo)
                    sse(
                        serialize = { typeInfo, value ->
                            val json: Json by application.dependencies
                            val serializer = json.serializersModule.serializer(typeInfo.kotlinType!!)
                            json.encodeToString(serializer, value)
                        },
                    ) {
                        val eventFlowManager: EventFlowManager by application.dependencies
                        val chatId = call.requireValidChatId()
                        eventFlowManager.use(chatId) { flow ->
                            // TODO chunk based on time
                            flow
                                .takeWhile { it !is Event.End }
                                .collect { event ->
                                    send(event)
                                }
                            send(Event.End)
                        }
                    }
                    // Post a message to the chat and await the response
                    post {
                        val eventFlowManager: EventFlowManager by application.dependencies
                        val chatId = call.requireValidChatId()
                        val userMessage = call.receive<UserMessage>()
                        eventFlowManager.use(chatId) { flow ->
                            val agent =
                                aiAgent<UserMessage, Unit>(
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
}

private fun ApplicationCall.requireUser(): User =
    checkNotNull(principal<User>()) {
        "User principal missing. Are you calling this in a non-authenticated endpoint?"
    }

private suspend fun ApplicationCall.requireValidChatId(): ChatId {
    val chatRepository: InMemoryChatRepository by application.dependencies
    val user = requireUser()
    val chatId = ChatId(request.queryParameters.requireUuid("ChatId"))
    if (!chatRepository.contains(user.id, chatId)) {
        throw NotFoundException("Chat not found for user")
    }
    return chatId
}

private fun Parameters.requireUuid(name: String): Uuid {
    val value = getOrFail<String>(name)
    return Uuid.parseOrNull(value) ?: throw BadRequestException("Query parameter $name is not a valid UUID")
}
