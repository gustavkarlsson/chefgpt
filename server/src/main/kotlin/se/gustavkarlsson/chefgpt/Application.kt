package se.gustavkarlsson.chefgpt

import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.ktor.Koog
import ai.koog.ktor.aiAgent
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.auth.basicAuthenticationCredentials
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.api.FileId
import se.gustavkarlsson.chefgpt.api.MessageFromAi
import se.gustavkarlsson.chefgpt.api.MessageFromUser
import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.User
import se.gustavkarlsson.chefgpt.chats.ChatId
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.tools.IngredientStore
import se.gustavkarlsson.chefgpt.tools.SpoonacularClient
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.uuid.Uuid

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: DEV_SERVER_PORT
    val anthropicApiKey = System.getenv("ANTHROPIC_API_KEY")
    val spoonacularApiKey = System.getenv("SPOONACULAR_API_KEY")
    val ingredientStorePath = Paths.get("ingredient-store.txt")
    val imageStorePath = Files.createTempDirectory("chefgpt-image-store")
    embeddedServer(
        factory = Netty,
        port = port,
        host = "0.0.0.0",
        module = { module(anthropicApiKey, spoonacularApiKey, ingredientStorePath, imageStorePath) },
    ).start(wait = true)
}

fun Application.module(
    anthropicApiKey: String,
    spoonacularApiKey: String,
    ingredientStorePath: Path,
    imageStorePath: Path,
) {
    // FIXME Use DI for config
    val imageStore = ImageStore(imageStorePath)
    val userRepository = InMemoryUserRepository()
    val chatRepository = InMemoryChatRepository()
    val chatHistoryProvider = InMemoryChatHistoryProvider()

    // Extra lenient in production
    val jsonConfig =
        Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = !developmentMode
            allowComments = !developmentMode
            allowTrailingComma = !developmentMode
            prettyPrint = developmentMode
        }
    install(ContentNegotiation) {
        json(jsonConfig)
    }

    install(Koog) {
        llm {
            anthropic(apiKey = anthropicApiKey)
        }
        agentConfig {
            registerTools {
                tools(IngredientStore(ingredientStorePath))
                tools(SpoonacularClient(spoonacularApiKey))
                tool(ExitTool)
            }
            install(ChatMemory) {
                this.chatHistoryProvider = chatHistoryProvider
            }
            prompt {
                """
                You are a culinary expert specialized in suggesting meal recipes
                based on the user's ingredients and preferences.
                Start by greeting the user and listing their ingredients.
                Ask them if they would like to add or remove something.
                Then, search recipes using the ingredients.
                Present each recipe found with a super short description and URL.
                When the used is satisfied, exit the conversation.
                """.trimIndent()
            }
        }
    }

    authentication {
        basic {
            validate { credentials ->
                userRepository.login(credentials.name, credentials.password)
            }
        }
    }

    routing {
        // FIXME extract to separate file
        post("/register") {
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
                    val user = call.requireUser()
                    val chat = chatRepository.create(user.id)
                    call.respond(HttpStatusCode.Created, chat.id.value.toString())
                }
                route("/{chatId}") {
                    // Upload an image to the chat and return the FileId
                    post("/images") {
                        val chatId = call.requireValidChatId(chatRepository)
                        val fileId = FileId.random()
                        imageStore.writeFile(chatId, fileId, call.receiveChannel())
                        call.respond(HttpStatusCode.Created, fileId.value.toString())
                    }

                    route("/messages") {
                        // Get the conversation history up to this point (empty if new convo)
                        get {
                            val chatId = call.requireValidChatId(chatRepository)
                            val messages = chatHistoryProvider.load(chatId.value.toString())
                            call.respond(HttpStatusCode.OK, messages) // FIXME map to API messages
                        }
                        // Post a message to the chat and await the response
                        post {
                            val chatId = call.requireValidChatId(chatRepository)
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
}

private fun ApplicationCall.requireUser(): User =
    checkNotNull(principal<User>()) {
        "User principal missing. Are you calling this in a non-authenticated endpoint?"
    }

private suspend fun ApplicationCall.requireValidChatId(chatRepository: InMemoryChatRepository): ChatId {
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
