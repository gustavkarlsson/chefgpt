package se.gustavkarlsson.chefgpt

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.accept
import io.ktor.client.request.basicAuth
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

class ChefGptClient(
    private val baseUrl: String = "http://localhost:8080",
    username: String,
    password: String,
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
            defaultRequest {
                basicAuth(username, password)
            }
        }

    suspend fun register(): Boolean {
        val response = httpClient.post("$baseUrl/register")
        return response.status.isSuccess()
    }

    suspend fun uploadImage(
        data: Path,
        contentType: ContentType,
    ): ImageUrl {
        val response =
            httpClient.post("$baseUrl/images") {
                this.contentType(contentType)
                accept(ContentType.Text.Plain)
                setBody(data.byteReadChannel())
            }
        val urlString = response.bodyAsText()
        return ImageUrl(urlString)
    }

    suspend fun createChat(): ChatId {
        // TODO Accept text/plain?
        val response =
            httpClient.post("$baseUrl/chats") {
                accept(ContentType.Text.Plain)
            }
        val uuidString = response.bodyAsText()
        return ChatId.parse(uuidString)
    }

    fun listenToEvents(chatId: ChatId): Flow<ApiEvent> =
        flow {
            httpClient.sse(
                urlString = "$baseUrl/chats/$chatId/events",
                deserialize = { typeInfo, text ->
                    val serializer = json.serializersModule.serializer(typeInfo.kotlinType!!)
                    json.decodeFromString(serializer, text)
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
        chatId: ChatId,
        action: ApiAction,
    ) {
        httpClient.post("$baseUrl/chats/$chatId/actions") {
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
