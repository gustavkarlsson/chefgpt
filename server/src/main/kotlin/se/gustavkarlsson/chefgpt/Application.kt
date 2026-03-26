package se.gustavkarlsson.chefgpt

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import java.nio.file.Paths

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    with(environment.config) {
        plugins(
            anthropicApiKey = property("chefgpt.anthropicApiKey").getString(),
            spoonacularApiKey = property("chefgpt.spoonacularApiKey").getString(),
            ingredientStorePath = Paths.get(property("chefgpt.ingredientStorePath").getString()),
            cloudinaryApiKey = property("chefgpt.cloudinary.apiKey").getString(),
            cloudinaryApiSecret = property("chefgpt.cloudinary.apiSecret").getString(),
            cloudinaryCloud = property("chefgpt.cloudinary.cloud").getString(),
        )
    }

    routing(Routing::routes)
}
