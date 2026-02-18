package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.ktor.Koog
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
import io.ktor.websocket.readText
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.tools.IngredientStore
import se.gustavkarlsson.chefgpt.tools.SpoonacularClient

fun main() {
    val anthropicApiKey = System.getenv("ANTHROPIC_API_KEY")
    val spoonacularApiKey = System.getenv("SPOONACULAR_API_KEY")
    val ingredientStorePath = Paths.get("ingredient-store.txt")
    val port = System.getenv("PORT")?.toInt() ?: DEV_SERVER_PORT
    embeddedServer(
        factory = Netty,
        port = port,
        host = "0.0.0.0",
        module = { module(anthropicApiKey, spoonacularApiKey, ingredientStorePath) }
    ).start(wait = true)
}

fun Application.module(anthropicApiKey: String, spoonacularApiKey: String, ingredientStorePath: Path) {
    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout = 60.seconds
        contentConverter = KotlinxWebsocketSerializationConverter(Json) // TODO configure for leniency
        extensions {
            install(WebSocketDeflateExtension)
        }
    }

    install(Koog) {
        llm {
            anthropic(apiKey = anthropicApiKey)
        }
        agentConfig {
            prompt {
                system(
                    """
                        You are a culinary expert specialized in suggesting meal recipes based on the user's ingredients and preferences.
                        Start by greeting the user with their name.
                        Check the ingredient store for any stored ingredients.
                        If there are none, ask the user what the have, and add them.
                        When some ingredients exist, search recipes using the ingredients.
                        Present each recipe found with a super short description and URL.
                        When the used is satisfied, exit the conversation.
                    """.trimIndent()
                )
            }
            registerTools {
                tools(IngredientStore(ingredientStorePath))
                tools(SpoonacularClient(spoonacularApiKey))
                tool(ExitTool)
            }
        }
    }

    routing {
        webSocket("/find-recipe-chat") {
            // FIXME create agent, define data models, and build the logic
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    outgoing.send(Frame.Text("YOU SAID: $text"))
                }
            }
        }
    }
}
