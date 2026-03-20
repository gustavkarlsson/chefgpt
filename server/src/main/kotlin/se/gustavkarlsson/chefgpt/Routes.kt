package se.gustavkarlsson.chefgpt

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.core.dsl.builder.strategy
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
import io.ktor.server.routing.Routing
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import se.gustavkarlsson.chefgpt.api.MessageFromAi
import se.gustavkarlsson.chefgpt.api.MessageFromUser
import se.gustavkarlsson.chefgpt.auth.User
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.chats.ChatId
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import kotlin.uuid.Uuid

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
                post("/images") {
                    val imageStore: ImageStore by application.dependencies
                    val chatId = call.requireValidChatId()
                    val fileId = imageStore.writeFile(chatId, call.receiveChannel())
                    call.respond(HttpStatusCode.Created, fileId.value.toString())
                }

                route("/messages") {
                    // Get the conversation history up to this point (empty if new convo)
                    get {
                        val chatHistoryProvider: ChatHistoryProvider by application.dependencies
                        val chatId = call.requireValidChatId()
                        val messages = chatHistoryProvider.load(chatId.value.toString())
                        call.respond(HttpStatusCode.OK, messages) // FIXME map to API messages
                    }
                    // Post a message to the chat and await the response
                    post {
                        val chatId = call.requireValidChatId()
                        val userMessage = call.receive<MessageFromUser>()
                        val agent =
                            aiAgent<MessageFromUser, MessageFromAi>(
                                strategy =
                                    strategy("find-recipe") {
                                        // FIXME implement strategy
                                    },
                                model = AnthropicModels.Haiku_4_5,
                            )
                        val messageFromAi = agent.run(userMessage, chatId.value.toString())
                        call.respond(HttpStatusCode.Created, messageFromAi)
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
