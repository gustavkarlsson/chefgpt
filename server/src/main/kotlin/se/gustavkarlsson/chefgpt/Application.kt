package se.gustavkarlsson.chefgpt

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.ktor.Koog
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.tools.IngredientStore
import se.gustavkarlsson.chefgpt.tools.SpoonacularClient
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
    val imageStore = ImageStore(imageStorePath)
    val userRepository = InMemoryUserRepository()
    val chatRepository = InMemoryChatRepository()
    val chatHistoryProvider = InMemoryChatHistoryProvider()

    dependencies {
        provide<ChatHistoryProvider> { chatHistoryProvider }
        provide<UserRepository> { userRepository }
        provide<ChatRepository> { chatRepository }
        provide<ImageStore> { imageStore }
    }

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

    routing(Routing::routes)
}
