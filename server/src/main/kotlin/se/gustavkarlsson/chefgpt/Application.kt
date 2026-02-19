package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketDeflateExtension
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.tools.IngredientStore
import se.gustavkarlsson.chefgpt.tools.SpoonacularClient
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds

fun main() {
    val anthropicApiKey = System.getenv("ANTHROPIC_API_KEY")
    val spoonacularApiKey = System.getenv("SPOONACULAR_API_KEY")
    val ingredientStorePath = Paths.get("ingredient-store.txt")
    val port = System.getenv("PORT")?.toInt() ?: DEV_SERVER_PORT
    embeddedServer(
        factory = Netty,
        port = port,
        host = "0.0.0.0",
        module = { module(anthropicApiKey, spoonacularApiKey, ingredientStorePath) },
    ).start(wait = true)
}

fun Application.module(
    anthropicApiKey: String,
    spoonacularApiKey: String,
    ingredientStorePath: Path,
) {
    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout = 60.seconds
        contentConverter = KotlinxWebsocketSerializationConverter(Json) // TODO configure for leniency
        extensions {
            install(WebSocketDeflateExtension)
        }
    }

    routing {
        webSocket("/find-recipe-chat") {
            val conversation = Conversation(this)
            val agent =
                AIAgent(
                    promptExecutor = simpleAnthropicExecutor(apiKey = anthropicApiKey),
                    llmModel = AnthropicModels.Haiku_4_5,
                    toolRegistry =
                        ToolRegistry {
                            tools(IngredientStore(ingredientStorePath))
                            tools(SpoonacularClient(anthropicApiKey))
                            tool(ExitTool)
                        },
                    systemPrompt =
                        """
                        You are a culinary expert specialized in suggesting meal recipes based on the user's ingredients and preferences.
                        Start by greeting the user with their name.
                        Check the ingredient store for any stored ingredients.
                        If there are none, ask the user what the have, and add them.
                        When some ingredients exist, search recipes using the ingredients.
                        Present each recipe found with a super short description and URL.
                        When the used is satisfied, exit the conversation.
                        """.trimIndent(),
                    strategy = findRecipeFunctionalStrategy(conversation),
                )
            agent.run(Unit)
            send(Frame.Close())
        }
    }
}
