package se.gustavkarlsson.chefgpt

import ai.koog.ktor.Koog
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.session
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.header
import io.ktor.server.sse.SSE
import io.ktor.util.toMap
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.slf4j.event.Level
import se.gustavkarlsson.chefgpt.agent.EventBackedChatMemory
import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.PostgresUserRepository
import se.gustavkarlsson.chefgpt.auth.Session
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.auth.registrationRules
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import se.gustavkarlsson.chefgpt.db.connectR2dbcDatabase
import se.gustavkarlsson.chefgpt.db.createSimpleDataSource
import se.gustavkarlsson.chefgpt.db.migrateDatabase
import se.gustavkarlsson.chefgpt.images.createCloudinaryImageUploader
import se.gustavkarlsson.chefgpt.tools.SpoonacularClient
import javax.sql.DataSource

fun Application.plugins(config: ApplicationConfig) {
    // Extra lenient in production
    val json =
        Json {
            encodeDefaults = true
            isLenient = !developmentMode
            explicitNulls = false
            ignoreUnknownKeys = !developmentMode
            allowComments = !developmentMode
            allowTrailingComma = !developmentMode
            prettyPrint = developmentMode
        }
    val chatRepository = InMemoryChatRepository()
    val eventRepository = InMemoryEventRepository()
    dependencies {
        provide { registrationRules }
        provide<DataSource> { createSimpleDataSource(config.config("database")) }
        provide(InMemoryUserRepository::class)
        provide {
            val dataSource = resolve<DataSource>()
            migrateDatabase(dataSource)
            connectR2dbcDatabase(config.config("database"))
        }
        provide(PostgresUserRepository::class)
        provide<UserRepository> { resolve<PostgresUserRepository>() }

        provide<ChatRepository> { chatRepository }
        provide<EventRepository> { eventRepository }
        provide { createCloudinaryImageUploader(config.config("chefgpt.cloudinary")) }
        provide { json }
        provide { SpoonacularClient(config.property("chefgpt.spoonacularApiKey").getString()) }
    }

    install(CallLogging) {
        val callConfig = config.config("logging.calls")
        val configLevelString = callConfig.propertyOrNull("level")?.getString()?.uppercase()
        val configLevel = Level.entries.find { it.name == configLevelString }
        level = configLevel ?: Level.INFO

        val requestHeaders = callConfig.propertyOrNull("request.headers")?.getString().toBoolean()
        if (requestHeaders) {
            mdc("request.headers") { call ->
                call.request.headers
                    .toMap()
                    .toString()
            }
        }

        val responseHeaders = callConfig.propertyOrNull("response.headers")?.getString().toBoolean()
        if (responseHeaders) {
            mdc("response.headers") { call ->
                call.response.headers
                    .allValues()
                    .toMap()
                    .toString()
            }
        }

        format { call ->
            buildString {
                append("${call.response.status()}: ")
                append(call.request.httpMethod)
                append(" ")
                append(call.request.path())
                append(" in ${call.processingTimeMillis()}ms")
            }
        }
    }

    install(ContentNegotiation) {
        json(json)
    }

    install(SSE)

    // TODO Install RateLimiting
    // TODO Install CallId
    // TODO Install RequestValidation to validate incoming (and outgoing?) data
    // TODO Install StatusPages to handle RequestValidationException
    // TODO Install Compression for compressed requests/responses
    // TODO Install DefaultHeaders to send default date headers and more
    // TODO Install ConditionalHeaders to not send the body of data that has not changed
    // TODO Install DataConversion to auto-convert data such as Uuid:s and dates
    // TODO Install HttpRequestLifecycle and set cancelCallOnClose = true to cancel requests that the client canceled
    // TODO Setup a docker image and install  Grafana-LGTM (https://ktor.io/docs/server-opentelemetry.html#verify-telemetry-data-with-grafana-lgtm)

    install(Koog) {
        llm {
            anthropic(apiKey = config.property("chefgpt.anthropicApiKey").getString())
        }
        agentConfig {
            prompt {
                system(
                    """
                    You are a culinary expert specialized finding the perfect recipe.
                    based on the user's ingredients, time of day, and mood.

                    Start by greeting the user. If they have not added any ingredients,
                    suggest that they do so using text or by taking a photo.

                    When there are ingredients, ask the user what they would like to cook,
                    and give a subtle hint based on the time of day.

                    Use the recipe tools together with the ingredient store
                    and other context to suggest some recipes.
                    Present each recipe found with a super short description and URL.

                    If there are too few results, suggest that the user updates their ingredients.

                    When the used has picked a recipe, send the recipe to them.
                    """.trimIndent(),
                )
            }
            install(EventBackedChatMemory) {
                this.eventRepository = eventRepository
            }
        }
    }

    install(Sessions) {
        header<Session>("Session-Id", SessionStorageMemory())
    }

    authentication {
        session<Session> {
            validate { session ->
                val userRepository: UserRepository by application.dependencies
                session.takeIf { it.user.name in userRepository }
            }
            challenge { call.respond(HttpStatusCode.Unauthorized) }
        }
    }
}
