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
import se.gustavkarlsson.chefgpt.api.ApiChat
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.ApiIngredientUpdate
import se.gustavkarlsson.chefgpt.api.ApiJob
import se.gustavkarlsson.chefgpt.api.ApiNewIngredient
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.ApiRecipeUpdate
import se.gustavkarlsson.chefgpt.api.ApiSaveSpoonacularRecipe
import se.gustavkarlsson.chefgpt.api.ApiScanRecipe
import se.gustavkarlsson.chefgpt.api.ApiUploadedFile
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.api.FILE_NAME_HEADER
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.api.JobId
import se.gustavkarlsson.chefgpt.api.JoinId
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.chefgpt.debug.Settings
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.sessions.UserCredentials
import se.gustavkarlsson.chefgpt.util.sseTyped
import io.ktor.client.plugins.logging.Logger as KtorLogger

private val log = Logger.withTag("${ChefGptClient::class.simpleName}")

class ChefGptClient(
    private val settings: Settings,
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
                post("$baseUrl/register") {
                    basicAuth(credentials.userName.value, credentials.password.value)
                }
            },
            readSafe = { SessionId(headers["Session-Id"]!!) },
        )

    suspend fun login(credentials: UserCredentials): Result<SessionId, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl/login") {
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
                post("$baseUrl/files") {
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
    ): Result<ApiJob, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl/ingredients/scan") {
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
    ): Result<ApiJob, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl/recipes/scan") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(ApiScanRecipe(attachments))
                }
            },
            readSafe = { body() },
        )

    suspend fun createChat(sessionId: SessionId): Result<ApiChat, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl/chats") {
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
                delete("$baseUrl/chats/$chatId") {
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
                    url("$baseUrl/chats")
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
                    url("$baseUrl/chats/$chatId/events")
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
                    url("$baseUrl/ingredients")
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
                post("$baseUrl/ingredients") {
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
                delete("$baseUrl/ingredients/$ingredientId") {
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
                patch("$baseUrl/ingredients/$ingredientId") {
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
                    url("$baseUrl/recipes")
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
                post("$baseUrl/recipes") {
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
                patch("$baseUrl/recipes/$recipeId") {
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
                post("$baseUrl/recipes/$recipeId/overwrite-original") {
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
                post("$baseUrl/recipes/$recipeId/save-as-copy") {
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
                delete("$baseUrl/recipes/$recipeId") {
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
                get("$baseUrl/recipes/$recipeId") {
                    sessionIdHeader(sessionId)
                    accept(ContentType.Application.Json)
                }
            },
            readSafe = { body<ApiRecipe>() },
        )

    // Sends a message that starts the chat agent, and returns the job to poll for completion.
    suspend fun sendAction(
        sessionId: SessionId,
        chatId: ChatId,
        message: ApiUserSendsMessage,
    ): Result<ApiJob, ClientError> =
        request(
            send = { baseUrl ->
                post("$baseUrl/chats/$chatId/actions") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(message)
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
                post("$baseUrl/chats/$chatId/actions") {
                    sessionIdHeader(sessionId)
                    contentType(ContentType.Application.Json)
                    setBody(ApiUserJoinedChat(joinId))
                }
            },
            readSafe = {},
        )

    suspend fun getJob(
        sessionId: SessionId,
        jobId: JobId,
    ): Result<ApiJob, ClientError> =
        request(
            send = { baseUrl ->
                get("$baseUrl/jobs/$jobId") {
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
            }.flatMap { response -> response.toResultSafe(readSafe) }
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
