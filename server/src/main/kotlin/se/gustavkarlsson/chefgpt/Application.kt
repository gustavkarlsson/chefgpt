package se.gustavkarlsson.chefgpt

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
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

fun main() {
    val anthropicApiKey = System.getenv("ANTHROPIC_API_KEY")
    val port = System.getenv("PORT")?.toInt() ?: DEV_SERVER_PORT
    embeddedServer(
        factory = Netty,
        port = port,
        host = "0.0.0.0",
        module = { module(anthropicApiKey) }
    ).start(wait = true)
}

fun Application.module(anthropicApiKey: String) {
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
            // FIXME configure
        }
    }

    routing {
        webSocket("/echo") {
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
