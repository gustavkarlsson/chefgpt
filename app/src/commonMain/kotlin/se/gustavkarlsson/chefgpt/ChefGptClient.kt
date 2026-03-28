package se.gustavkarlsson.chefgpt

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.accept
import io.ktor.client.request.basicAuth
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
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
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.SessionId

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
        }

    suspend fun register(
        username: String,
        password: String,
    ): SessionId {
        val response =
            httpClient.post("$baseUrl/register") {
                basicAuth(username, password)
            }
        check(response.status.isSuccess()) { "Registration failed" }
        return SessionId.parse(response.bodyAsText())
    }

    suspend fun login(
        username: String,
        password: String,
    ): SessionId {
        val response =
            httpClient.post("$baseUrl/login") {
                basicAuth(username, password)
            }
        check(response.status.isSuccess()) { "Login failed" }
        return SessionId.parse(response.bodyAsText())
    }

    suspend fun uploadImage(
        sessionId: SessionId,
        data: Path,
        contentType: ContentType,
    ): ImageUrl {
        val response =
            httpClient.post("$baseUrl/images") {
                bearerAuth(sessionId.toString())
                this.contentType(contentType)
                accept(ContentType.Text.Plain)
                setBody(data.byteReadChannel())
            }
        val urlString = response.bodyAsText()
        return ImageUrl(urlString)
    }

    suspend fun createChat(sessionId: SessionId): ChatId {
        val response =
            httpClient.post("$baseUrl/chats") {
                bearerAuth(sessionId.toString())
                accept(ContentType.Text.Plain)
            }
        val uuidString = response.bodyAsText()
        return ChatId.parse(uuidString)
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
                    bearerAuth(sessionId.toString())
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
    ) {
        httpClient.post("$baseUrl/chats/$chatId/actions") {
            bearerAuth(sessionId.toString())
            contentType(ContentType.Application.Json)
            setBody(action)
        }
    }

    override fun close() {
        httpClient.close()
    }
}

private fun Path.byteReadChannel(): ByteReadChannel {
    val source = SystemFileSystem.source(this)
    return ByteReadChannel(source.buffered())
}
