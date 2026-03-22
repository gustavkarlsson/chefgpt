package se.gustavkarlsson.chefgpt

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import java.nio.file.Paths

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: DEV_SERVER_PORT
    embeddedServer(
        factory = Netty,
        port = port,
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    plugins(
        anthropicApiKey = System.getenv("ANTHROPIC_API_KEY"),
        spoonacularApiKey = System.getenv("SPOONACULAR_API_KEY"),
        ingredientStorePath = Paths.get("ingredient-store.txt"),
        imghippoApiKey = System.getenv("IMGHIPPO_API_KEY"),
    )
    routing(Routing::routes)
}
