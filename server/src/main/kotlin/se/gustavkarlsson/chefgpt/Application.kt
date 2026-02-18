package se.gustavkarlsson.chefgpt

import ai.koog.agents.ext.agent.reActStrategy
import ai.koog.ktor.Koog
import ai.koog.ktor.aiAgent
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

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
    install(Koog) {
        llm {
            anthropic(apiKey = anthropicApiKey)
        }
        agentConfig {
            // FIXME configure
        }
    }

    // FIXME set up the AI with websocket streaming
    routing {
        route("/ai") {
            post("/chat") {
                val userInput = call.receive<String>()
                val output = aiAgent(
                    strategy = reActStrategy(),
                    model = OpenAIModels.Chat.GPT4_1,
                    input = userInput
                )
                call.respond(HttpStatusCode.OK, output)
            }
        }
    }
}