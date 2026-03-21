package se.gustavkarlsson.chefgpt

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.ktor.Koog
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.UserRegistrationRule
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.tools.IngredientStore
import se.gustavkarlsson.chefgpt.tools.SpoonacularClient
import java.nio.file.Path

fun Application.plugins(
    anthropicApiKey: String,
    spoonacularApiKey: String,
    ingredientStorePath: Path,
    imageStorePath: Path,
) {
    val imageStore = ImageStore(imageStorePath)
    val userRepository =
        InMemoryUserRepository(
            rules =
                listOf(
                    UserRegistrationRule.Companion.name("Username must be at least 3 characters long") { name ->
                        name.length < 3
                    },
                    UserRegistrationRule.Companion.name("Username must start with a letter") { name ->
                        name.firstOrNull()?.isLetter() ?: false
                    },
                    UserRegistrationRule.Companion.name("Username must only contain letters and digits") { name ->
                        name.all { it.isLetterOrDigit() }
                    },
                    UserRegistrationRule.Companion.password("Password must be at least 8 characters") { password ->
                        password.length >= 8
                    },
                    UserRegistrationRule.Companion.password("Password must contain only valid characters") { password ->
                        password.none { it.isISOControl() } && password.all { it.isDefined() }
                    },
                    UserRegistrationRule.Companion.password(
                        "Password must contain at least three of the following: lower-case letter, upper-case letter, number, special character",
                    ) { password ->
                        // FIXME better algorithm for complexity
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
    val chatRepository = InMemoryChatRepository()
    val chatHistoryProvider = InMemoryChatHistoryProvider()

    dependencies {
        provide<ChatHistoryProvider> { chatHistoryProvider }
        provide<UserRepository> { userRepository }
        provide<ChatRepository> { chatRepository }
        provide<ImageStore> { imageStore }
    }

    // Extra lenient in production
    val jsonConfig =
        Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = !developmentMode
            allowComments = !developmentMode
            allowTrailingComma = !developmentMode
            prettyPrint = developmentMode
        }
    install(ContentNegotiation) {
        json(jsonConfig)
    }

    // FIXME Install RateLimiting
    // FIXME Install CallLogging
    // FIXME Install CallId
    // FIXME Install RequestValidation to validate incoming (and outgoing?) data
    // FIXME Install StatusPages to handle RequestValidationException
    // FIXME Install Compression for compressed requests/responses
    // FIXME Install DefaultHeaders to send default date headers and more
    // FIXME Install ConditionalHeaders to not send the body of data that has not changed
    // FIXME Install DataConversion to auto-convert data such as Uuid:s and dates
    // FIXME Install HttpRequestLifecycle and set cancelCallOnClose = true to cancel requests that the client canceled
    // FIXME Setup a docker image and install  Grafana-LGTM (https://ktor.io/docs/server-opentelemetry.html#verify-telemetry-data-with-grafana-lgtm)
    // FIXME Setup a database/databases

    install(Koog.Companion) {
        llm {
            anthropic(apiKey = anthropicApiKey)
        }
        agentConfig {
            registerTools {
                tools(IngredientStore(ingredientStorePath))
                tools(SpoonacularClient(spoonacularApiKey))
                tool(ExitTool)
            }
            install(ChatMemory.Feature) {
                this.chatHistoryProvider = chatHistoryProvider
            }
            prompt {
                """
                You are a culinary expert specialized in suggesting meal recipes
                based on the user's ingredients and preferences.
                Start by greeting the user and listing their ingredients.
                Ask them if they would like to add or remove something.
                Then, search recipes using the ingredients.
                Present each recipe found with a super short description and URL.
                When the used is satisfied, exit the conversation.
                """.trimIndent()
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
