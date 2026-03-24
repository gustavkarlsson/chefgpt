package se.gustavkarlsson.chefgpt

import ai.koog.ktor.Koog
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.sse.SSE
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.agent.EventBackedChatMemory
import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.UserRegistrationRule
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.images.CloudinaryImageUploader
import se.gustavkarlsson.chefgpt.images.ImageUploader
import se.gustavkarlsson.chefgpt.tools.IngredientStore
import se.gustavkarlsson.chefgpt.tools.SpoonacularClient
import java.nio.file.Path

fun Application.plugins(
    anthropicApiKey: String,
    spoonacularApiKey: String,
    ingredientStorePath: Path,
    cloudinaryApiKey: String,
    cloudinaryApiSecret: String,
    cloudinaryCloud: String,
) {
    val userRepository =
        InMemoryUserRepository(
            rules =
                listOf(
                    UserRegistrationRule.name("Username must be at least 3 characters long") { name ->
                        name.length >= 3
                    },
                    UserRegistrationRule.name("Username must start with a letter") { name ->
                        name.firstOrNull()?.isLetter() ?: false
                    },
                    UserRegistrationRule.name("Username must only contain letters and digits") { name ->
                        name.all { it.isLetterOrDigit() }
                    },
                    UserRegistrationRule.password("Password must be at least 8 characters") { password ->
                        password.length >= 8
                    },
                    UserRegistrationRule.password("Password must contain only valid characters") { password ->
                        password.none { it.isISOControl() } && password.all { it.isDefined() }
                    },
                    UserRegistrationRule.password(
                        "Password must contain at least three of the following: lower-case letter, upper-case letter, number, special character",
                    ) { password ->
                        // TODO Set a better algorithm for complexity
                        val criteriaCount =
                            listOf<Char.() -> Boolean>(
                                { isLowerCase() },
                                { isUpperCase() },
                                { isDigit() },
                                { !isLetterOrDigit() },
                            ).count { isCharCriteria ->
                                password.any(isCharCriteria)
                            }
                        criteriaCount >= 3
                    },
                ),
        )
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
    val ingredientStore = IngredientStore(ingredientStorePath)
    val spoonacularClient = SpoonacularClient(spoonacularApiKey)
    val chatRepository = InMemoryChatRepository()
    dependencies {
        provide<UserRepository> { userRepository }
        provide<ChatRepository> { chatRepository }
        provide<ImageUploader> { CloudinaryImageUploader(cloudinaryApiKey, cloudinaryApiSecret, cloudinaryCloud) }
        provide<Json> { json }
        provide<IngredientStore> { ingredientStore }
        provide<SpoonacularClient> { spoonacularClient }
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
    // TODO Setup a database/databases

    install(Koog) {
        llm {
            anthropic(apiKey = anthropicApiKey)
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
                userRepository.login(credentials.name, credentials.password)
            }
        }
    }
}
