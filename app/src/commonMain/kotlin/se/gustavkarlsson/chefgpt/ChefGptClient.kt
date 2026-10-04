package se.gustavkarlsson.chefgpt

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.flatMapEither
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.basicAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.JsonElement
import se.gustavkarlsson.chefgpt.api.auth.v1.AUTH_V1_PATH
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiAction
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiChat
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiEvent
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.chats.v1.CHATS_V1_PATH
import se.gustavkarlsson.chefgpt.api.common.CLIENT_PLATFORM_HEADER
import se.gustavkarlsson.chefgpt.api.common.CLIENT_VERSION_HEADER
import se.gustavkarlsson.chefgpt.api.common.ChatId
import se.gustavkarlsson.chefgpt.api.common.EventId
import se.gustavkarlsson.chefgpt.api.common.FILE_NAME_HEADER
import se.gustavkarlsson.chefgpt.api.common.IngredientId
import se.gustavkarlsson.chefgpt.api.common.JobId
import se.gustavkarlsson.chefgpt.api.common.JoinId
import se.gustavkarlsson.chefgpt.api.common.RecipeId
import se.gustavkarlsson.chefgpt.api.common.SpoonacularId
import se.gustavkarlsson.chefgpt.api.errors.v1.ApiError
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiUserFacts
import se.gustavkarlsson.chefgpt.api.facts.v1.FACTS_V1_PATH
import se.gustavkarlsson.chefgpt.api.files.v1.ApiUploadedFile
import se.gustavkarlsson.chefgpt.api.files.v1.FILES_V1_PATH
import se.gustavkarlsson.chefgpt.api.ingredients.v1.ApiIngredient
import se.gustavkarlsson.chefgpt.api.ingredients.v1.ApiIngredientUpdate
import se.gustavkarlsson.chefgpt.api.ingredients.v1.ApiNewIngredient
import se.gustavkarlsson.chefgpt.api.ingredients.v1.INGREDIENTS_V1_PATH
import se.gustavkarlsson.chefgpt.api.jobs.v1.ApiJob
import se.gustavkarlsson.chefgpt.api.jobs.v1.JOBS_V1_PATH
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiRecipe
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiRecipeUpdate
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiSaveSpoonacularRecipe
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiScanRecipe
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiScrapeRecipe
import se.gustavkarlsson.chefgpt.api.recipes.v1.RECIPES_V1_PATH
import se.gustavkarlsson.chefgpt.debug.Settings
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.sessions.UserCredentials
import se.gustavkarlsson.chefgpt.updates.UpdateRequiredNotifier
import se.gustavkarlsson.chefgpt.util.sseTyped
import io.ktor.client.plugins.logging.Logger as KtorLogger

private val log = Logger.withTag("${ChefGptClient::class.simpleName}")

class ChefGptClient(
    private val settings: Settings,
    private val updateRequiredNotifier: UpdateRequiredNotifier,
    developmentMode: Boolean = false,
) : AutoCloseable {
    private val json = chefGptJson(strict = developmentMode, prettyPrint = developmentMode)

    private val httpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(SSE)
            install(HttpTimeout)
            defaultRequest {
                // Lets the server apply client-specific workarounds.
                header(CLIENT_PLATFORM_HEADER, devicePlatform.clientHeaderValue)
                header(CLIENT_VERSION_HEADER, CLIENT_VERSION)
            }

            install(Logging) {
                logger =
                    object : KtorLogger {
                        private val log = Logger.withTag("${ChefGptClient::class.simpleName}-Calls")

                        override fun log(message: String) {
                            log.d { message }
                        }
                    }
                format
                // TODO Make level configurable
                level = LogLevel.HEADERS
            }
        }

    suspend fun register(credentials: UserCredentials): Result<SessionId, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$AUTH_V1_PATH/register") {
                    basicAuth(credentials.userName.value, credentials.password.value)
                }
            },
            readSafe = { SessionId(headers["Session-Id"]!!) },
        )

    suspend fun login(credentials: UserCredentials): Result<SessionId, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$AUTH_V1_PATH/login") {
                    basicAuth(credentials.userName.value, credentials.password.value)
                }
            },
            readSafe = { SessionId(headers["Session-Id"]!!) },
        )

    suspend fun uploadFile(
        sessionId: SessionId,
        data: Path,
        contentType: ContentType,
    ): Result<ApiUploadedFile, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$FILES_V1_PATH") {
                    sessionIdHeader(sessionId)
                    contentType(contentType)
                    header(FILE_NAME_HEADER, data.name)
                    accept(ContentType.Application.Json)
                    setBody(data.byteReadChannel())
                }
            },
            readSafe = { body() },
        )

    // Starts an ingredient scan and returns the job to poll for its result.
    suspend fun scanIngredients(
        sessionId: SessionId,
        data: Path,
        contentType: ContentType,
    ): Result<ApiJob<List<String>>, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$INGREDIENTS_V1_PATH/scan") {
                    sessionIdHeader(sessionId)
                    contentType(contentType)
                    accept(ContentType.Application.Json)
                    setBody(data.byteReadChannel())
                }
            },
            readSafe = { body() },
        )

    // Starts a recipe scan and returns the job to poll for its result.
    suspend fun scanRecipes(
        sessionId: SessionId,
        attachments: List<ApiUploadedFile>,
    ): Result<ApiJob<List<RecipeId>>, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$RECIPES_V1_PATH/scan") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(ApiScanRecipe(attachments))
                }
            },
            readSafe = { body() },
        )

    // Starts a recipe scrape and returns the job to poll for its result.
    suspend fun scrapeRecipe(
        sessionId: SessionId,
        url: String,
    ): Result<ApiJob<List<RecipeId>>, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$RECIPES_V1_PATH/scrape") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(ApiScrapeRecipe(url))
                }
            },
            readSafe = { body() },
        )

    suspend fun createChat(sessionId: SessionId): Result<ApiChat, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$CHATS_V1_PATH") {
                    sessionIdHeader(sessionId)
                    accept(ContentType.Application.Json)
                }
            },
            readSafe = { body<ApiChat>() },
        )

    suspend fun deleteChat(
        sessionId: SessionId,
        chatId: ChatId,
    ): Result<Unit, ClientError> =
        request(
            send = { baseUrl ->
                delete("$baseUrl$CHATS_V1_PATH/$chatId") {
                    sessionIdHeader(sessionId)
                    accept(ContentType.Application.Json)
                }
            },
            readSafe = {},
        )

    fun listenToChats(sessionId: SessionId): Flow<List<ApiChat>> =
        channelFlow {
            val baseUrl = settings.getBaseUrl()
            httpClient.sseTyped<List<ApiChat>>(
                json = json,
                eventType = "chats",
                request = {
                    url("$baseUrl$CHATS_V1_PATH")
                    sessionIdHeader(sessionId)
                },
            ) { _, incoming ->
                incoming.collect(::send)
            }
        }

    // TODO Error handling
    fun listenToEvents(
        sessionId: SessionId,
        chatId: ChatId,
        lastEventId: EventId?,
    ): Flow<ApiEvent> =
        channelFlow {
            val baseUrl = settings.getBaseUrl()
            httpClient.sseTyped<ApiEvent>(
                json = json,
                eventType = "event",
                request = {
                    url("$baseUrl$CHATS_V1_PATH/$chatId/events")
                    if (lastEventId != null) {
                        parameter("lastEventId", lastEventId)
                    }
                    sessionIdHeader(sessionId)
                },
            ) { _, incoming ->
                incoming.collect(::send)
            }
        }

    // TODO Error handling
    fun listenToIngredients(sessionId: SessionId): Flow<List<ApiIngredient>> =
        channelFlow {
            val baseUrl = settings.getBaseUrl()
            httpClient.sseTyped<List<ApiIngredient>>(
                json = json,
                eventType = "ingredients",
                request = {
                    url("$baseUrl$INGREDIENTS_V1_PATH")
                    sessionIdHeader(sessionId)
                },
            ) { _, incoming ->
                incoming.collect(::send)
            }
        }

    suspend fun createIngredient(
        sessionId: SessionId,
        name: String,
    ): Result<Unit, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$INGREDIENTS_V1_PATH") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    setBody(ApiNewIngredient(name))
                }
            },
            readSafe = {},
        )

    suspend fun destroyIngredient(
        sessionId: SessionId,
        ingredientId: IngredientId,
    ): Result<Unit, ClientError> =
        request(
            send = { baseUrl ->
                delete("$baseUrl$INGREDIENTS_V1_PATH/$ingredientId") {
                    sessionIdHeader(sessionId)
                }
            },
            readSafe = {},
        )

    suspend fun setIngredientInventory(
        sessionId: SessionId,
        ingredientId: IngredientId,
        inInventory: Boolean,
    ): Result<Unit, ClientError> =
        request(
            send = { baseUrl ->
                patch("$baseUrl$INGREDIENTS_V1_PATH/$ingredientId") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    setBody(ApiIngredientUpdate(inInventory))
                }
            },
            readSafe = {},
        )

    // TODO Error handling
    fun listenToRecipeSummaries(sessionId: SessionId): Flow<List<ApiRecipeSummary>> =
        channelFlow {
            val baseUrl = settings.getBaseUrl()
            httpClient.sseTyped<List<ApiRecipeSummary>>(
                json = json,
                eventType = "recipes",
                request = {
                    url("$baseUrl$RECIPES_V1_PATH")
                    sessionIdHeader(sessionId)
                },
            ) { _, incoming ->
                incoming.collect(::send)
            }
        }

    // Saves a recipe by its Spoonacular id. The server looks the recipe
    // up before storing it and returns the stored recipe.
    suspend fun saveRecipe(
        sessionId: SessionId,
        spoonacularId: SpoonacularId,
    ): Result<ApiRecipe, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$RECIPES_V1_PATH") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(ApiSaveSpoonacularRecipe(spoonacularId))
                }
            },
            readSafe = { body<ApiRecipe>() },
        )

    suspend fun setRecipeFavorite(
        sessionId: SessionId,
        recipeId: RecipeId,
        favorite: Boolean,
    ): Result<Unit, ClientError> =
        request(
            send = { baseUrl ->
                patch("$baseUrl$RECIPES_V1_PATH/$recipeId") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    setBody(ApiRecipeUpdate(favorite))
                }
            },
            readSafe = {},
        )

    // Lets a modified recipe replace the recipe it was modified from, deleting that one.
    suspend fun overwriteOriginalRecipe(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<ApiRecipe, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$RECIPES_V1_PATH/$recipeId/overwrite-original") {
                    sessionIdHeader(sessionId)
                    accept(ContentType.Application.Json)
                }
            },
            readSafe = { body<ApiRecipe>() },
        )

    // Keeps a modified recipe alongside the recipe it was modified from.
    suspend fun saveRecipeAsCopy(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<ApiRecipe, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$RECIPES_V1_PATH/$recipeId/save-as-copy") {
                    sessionIdHeader(sessionId)
                    accept(ContentType.Application.Json)
                }
            },
            readSafe = { body<ApiRecipe>() },
        )

    suspend fun deleteRecipe(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Unit, ClientError> =
        request(
            send = { baseUrl ->
                delete("$baseUrl$RECIPES_V1_PATH/$recipeId") {
                    sessionIdHeader(sessionId)
                }
            },
            readSafe = {},
        )

    suspend fun getRecipe(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<ApiRecipe, ClientError> =
        request(
            send = { baseUrl ->
                get("$baseUrl$RECIPES_V1_PATH/$recipeId") {
                    sessionIdHeader(sessionId)
                    accept(ContentType.Application.Json)
                }
            },
            readSafe = { body<ApiRecipe>() },
        )

    suspend fun getFacts(sessionId: SessionId): Result<ApiUserFacts, ClientError> =
        request(
            send = { baseUrl ->
                get("$baseUrl$FACTS_V1_PATH") {
                    sessionIdHeader(sessionId)
                    accept(ContentType.Application.Json)
                }
            },
            readSafe = { body<ApiUserFacts>() },
        )

    suspend fun putFacts(
        sessionId: SessionId,
        facts: ApiUserFacts,
    ): Result<ApiUserFacts, ClientError> =
        request(
            send = { baseUrl ->
                put("$baseUrl$FACTS_V1_PATH") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(facts)
                }
            },
            readSafe = { body<ApiUserFacts>() },
        )

    // Sends a message that starts the chat agent, and returns the job to poll for completion.
    suspend fun sendAction(
        sessionId: SessionId,
        chatId: ChatId,
        message: ApiUserSendsMessage,
    ): Result<ApiJob<Unit>, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$CHATS_V1_PATH/$chatId/actions") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody<ApiAction>(message)
                }
            },
            readSafe = { body() },
        )

    // Joins a chat. This starts no agent, so it completes synchronously.
    suspend fun joinChat(
        sessionId: SessionId,
        chatId: ChatId,
        joinId: JoinId,
    ): Result<Unit, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl$CHATS_V1_PATH/$chatId/actions") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    setBody<ApiAction>(ApiUserJoinedChat(joinId))
                }
            },
            readSafe = {},
        )

    suspend fun getJob(
        sessionId: SessionId,
        jobId: JobId,
    ): Result<ApiJob<JsonElement>, ClientError> =
        request(
            send = { baseUrl ->
                get("$baseUrl$JOBS_V1_PATH/$jobId") {
                    sessionIdHeader(sessionId)
                    accept(ContentType.Application.Json)
                }
            },
            readSafe = { body() },
        )

    // Runs the request, turning any failure — connection problems included —
    // into a ClientError rather than letting it propagate as a crash.
    private suspend fun <T> request(
        send: suspend HttpClient.(baseUrl: String) -> HttpResponse,
        readSafe: suspend HttpResponse.() -> T,
    ): Result<T, ClientError> {
        val baseUrl = settings.getBaseUrl()
        return runCatching { httpClient.send(baseUrl) }
            .mapError { error ->
                log.e(error) { "Request failed" }
                ClientError.Other
            }.flatMap { response ->
                if (response.status == HttpStatusCode.Gone) {
                    updateRequiredNotifier.notifyUpdateRequired()
                }
                response.toResultSafe(readSafe)
            }
    }

    override fun close() {
        httpClient.close()
    }
}

private suspend fun <T> HttpResponse.toResultSafe(readSafe: suspend HttpResponse.() -> T): Result<T, ClientError> =
    if (status.isSuccess()) {
        runCatching { readSafe() }.mapError { null }
    } else {
        runCatching { body<ApiError?>() }.flatMapEither(
            success = { Err(it) }, // ApiError becomes the failure case
            failure = { Err(null) }, // Throwables become null failure data
        )
    }.mapError { body ->
        ClientError.Http(status, body)
    }

private fun HttpRequestBuilder.sessionIdHeader(sessionId: SessionId) {
    header("Session-Id", sessionId.value)
}

private fun Path.byteReadChannel(): ByteReadChannel {
    val source = SystemFileSystem.source(this)
    return ByteReadChannel(source.buffered())
}

sealed interface ClientError {
    // The server responded with a non-success status.
    data class Http(
        val status: HttpStatusCode,
        val errorBody: ApiError?,
    ) : ClientError

    // The request never produced a usable response (e.g. connection failure).
    data object Other : ClientError
}
