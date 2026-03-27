package se.gustavkarlsson.chefgpt

import ai.koog.ktor.Koog
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.sse.SSE
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import se.gustavkarlsson.chefgpt.agent.EventBackedChatMemory
import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.PostgresUserRepository
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.auth.registrationRules
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.db.createHikariDataSource
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
    dependencies {
        provide { registrationRules }
        provide<DataSource> { createHikariDataSource(config.config("hikari")) }
        provide(InMemoryUserRepository::class)
        provide {
            val dataSource = resolve<DataSource>()
            migrateDatabase(dataSource) // Migrate before creating the database wrapper
            Database.connect(dataSource)
        }
        provide(PostgresUserRepository::class)
        provide<UserRepository> { resolve<PostgresUserRepository>() }

        provide<ChatRepository> { chatRepository }
        provide { createCloudinaryImageUploader(config.config("chefgpt.cloudinary")) }
        provide { json }
        provide { SpoonacularClient(config.property("chefgpt.spoonacularApiKey").getString()) }
    }

    install(CallLogging)

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
                this.chatRepository = chatRepository
            }
        }
    }

    authentication {
        basic {
            validate { credentials ->
                val userRepository: UserRepository by application.dependencies
                userRepository.login(credentials.name, credentials.password)
            }
        }
    }
}
