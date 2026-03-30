package se.gustavkarlsson.chefgpt

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMapEither
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.basicAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiChat
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.ImageUrl
import io.ktor.client.plugins.logging.Logger as KtorLogger

class ChefGptClient(
    private val baseUrl: String = "http://localhost:8080",
    developmentMode: Boolean = false,
) : AutoCloseable {
    private val json =
        Json {
            encodeDefaults = true
            isLenient = !developmentMode
            explicitNulls = false
            ignoreUnknownKeys = !developmentMode
            allowComments = !developmentMode
            allowTrailingComma = !developmentMode
            prettyPrint = developmentMode
        }

    private val httpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(SSE)

            install(Logging) {
                logger =
                    object : KtorLogger {
                        private val log = Logger.withTag("${ChefGptClient::class.simpleName}-Calls")

                        override fun log(message: String) {
                            log.i { message }
                        }
                    }
                // TODO Make level configurable
                level = LogLevel.HEADERS
            }
        }

    suspend fun register(
        username: String,
        password: String,
    ): Result<SessionId, ErrorResponse> {
        val response =
            httpClient.post("$baseUrl/register") {
                basicAuth(username, password)
            }
        return response.toResultSafe {
            SessionId(response.headers["Session-Id"]!!)
        }
    }

    suspend fun login(
        username: String,
        password: String,
    ): Result<SessionId, ErrorResponse> {
        val response =
            httpClient.post("$baseUrl/login") {
                basicAuth(username, password)
            }
        return response.toResultSafe {
            SessionId(response.headers["Session-Id"]!!)
        }
    }

    suspend fun uploadImage(
        sessionId: SessionId,
        data: Path,
        contentType: ContentType,
    ): Result<ImageUrl, ErrorResponse> {
        val response =
            httpClient.post("$baseUrl/images") {
                sessionIdHeader(sessionId)
                contentType(contentType)
                accept(ContentType.Text.Plain)
                setBody(data.byteReadChannel())
            }
        return response.toResultSafe {
            val urlString = response.bodyAsText()
            ImageUrl(urlString)
        }
    }

    suspend fun createChat(sessionId: SessionId): Result<ChatId, ErrorResponse> {
        val response =
            httpClient.post("$baseUrl/chats") {
                sessionIdHeader(sessionId)
                accept(ContentType.Application.Json)
            }
        return response.toResultSafe {
            response.body<ApiChat>().id
        }
    }

    fun listenToEvents(
        sessionId: SessionId,
        chatId: ChatId,
    ): Flow<ApiEvent> =
        flow {
            httpClient.sse(
                urlString = "$baseUrl/chats/$chatId/events",
                deserialize = { typeInfo, text ->
                    val serializer = json.serializersModule.serializer(typeInfo.kotlinType!!)
                    json.decodeFromString(serializer, text)
                },
                request = {
                    sessionIdHeader(sessionId)
                },
            ) {
                incoming.collect { serverSentEvent ->
                    val data = serverSentEvent.data ?: return@collect
                    if (data == "heartbeat") return@collect
                    val event = json.decodeFromString<ApiEvent>(data)
                    emit(event)
                }
            }
        }

    suspend fun sendAction(
        sessionId: SessionId,
        chatId: ChatId,
        action: ApiAction,
    ): Result<Nothing?, ErrorResponse> {
        val response =
            httpClient.post("$baseUrl/chats/$chatId/actions") {
                sessionIdHeader(sessionId)
                contentType(ContentType.Application.Json)
                setBody(action)
            }
        return response.toResultSafe { null }
    }

    override fun close() {
        httpClient.close()
    }
}

private suspend fun <T> HttpResponse.toResultSafe(readSafe: suspend HttpResponse.() -> T): Result<T, ErrorResponse> =
    if (status.isSuccess()) {
        runCatching { readSafe() }.mapError { null }
    } else {
        runCatching { body<ApiError?>() }.flatMapEither(
            success = { Err(it) }, // ApiError becomes the failure case
            failure = { Err(null) }, // Throwables become null failure data
        )
    }.mapError { body ->
        ErrorResponse(status, body)
    }

private fun HttpRequestBuilder.sessionIdHeader(sessionId: SessionId) {
    header("Session-Id", sessionId.value.toString())
}

private fun Path.byteReadChannel(): ByteReadChannel {
    val source = SystemFileSystem.source(this)
    return ByteReadChannel(source.buffered())
}

data class ErrorResponse(
    val status: HttpStatusCode,
    val errorBody: ApiError? = null,
)
