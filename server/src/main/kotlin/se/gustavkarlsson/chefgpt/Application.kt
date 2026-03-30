package se.gustavkarlsson.chefgpt

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import se.gustavkarlsson.chefgpt.plugins.installAuthentication
import se.gustavkarlsson.chefgpt.plugins.installCallLogging
import se.gustavkarlsson.chefgpt.plugins.installContentNegotiation
import se.gustavkarlsson.chefgpt.plugins.installDependencies
import se.gustavkarlsson.chefgpt.plugins.installKoog
import se.gustavkarlsson.chefgpt.plugins.installSSE
import se.gustavkarlsson.chefgpt.plugins.installSessions
import se.gustavkarlsson.chefgpt.setup.createAiAgent
import se.gustavkarlsson.chefgpt.setup.createChatRepository
import se.gustavkarlsson.chefgpt.setup.createEventRepository
import se.gustavkarlsson.chefgpt.setup.createImageUploader
import se.gustavkarlsson.chefgpt.setup.createJson
import se.gustavkarlsson.chefgpt.setup.createPostgresAccessOrNull
import se.gustavkarlsson.chefgpt.setup.createRethinkDbAccessOrNull
import se.gustavkarlsson.chefgpt.setup.createSessionStorage
import se.gustavkarlsson.chefgpt.setup.createSpoonacularClient
import se.gustavkarlsson.chefgpt.setup.createUserRepository

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val config = environment.config
    val database = createPostgresAccessOrNull(config)
    val rethinkDb = createRethinkDbAccessOrNull(config)
    val userRepository = createUserRepository(database)
    val chatRepository = createChatRepository(database)
    val eventRepository = createEventRepository(rethinkDb)
    val sessionStorage = createSessionStorage(database)
    val imageUploader =
        createImageUploader(
            type = config.property("chefgpt.imageUploader").getString(),
            config = config,
        )
    val spoonacularClient = createSpoonacularClient(config.property("chefgpt.spoonacularApiKey").getString())
    val aiAgent = createAiAgent(config.property("chefgpt.agent").getString())
    val json = createJson(developmentMode)

    installDependencies(
        database = database,
        userRepository = userRepository,
        chatRepository = chatRepository,
        eventRepository = eventRepository,
        imageUploader = imageUploader,
        spoonacularClient = spoonacularClient,
        aiAgent = aiAgent,
        json = json,
    )
    installCallLogging(config)
    installContentNegotiation(json)
    installSSE()
    installKoog(
        anthropicApiKey = config.property("chefgpt.anthropicApiKey").getString(),
        eventRepository = eventRepository,
    )
    installSessions(sessionStorage)
    installAuthentication(userRepository)
    routing(Routing::routes)
}
