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
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.Event
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.UserEvent
import kotlin.uuid.Uuid

class ChefGptClient(
    private val baseUrl: String = "http://localhost:8080",
    username: String = Uuid.random().toString(),
    password: String = "Pa55w0rd!!!!!",
    developmentMode: Boolean = false,
) {
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
        data: ByteReadChannel,
        contentType: ContentType,
    ): ImageUrl {
        val response =
            httpClient.post("$baseUrl/images") {
                this.contentType(contentType)
                accept(ContentType.Text.Plain)
                setBody(data)
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

    fun listenToEvents(chatId: ChatId): Flow<Event> =
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
                    val event = json.decodeFromString<Event>(data)
                    emit(event)
                }
            }
        }

    suspend fun sendEvent(
        chatId: ChatId,
        event: UserEvent,
    ) {
        httpClient.post("$baseUrl/chats/$chatId/events") {
            contentType(ContentType.Application.Json)
            setBody(event)
        }
    }
}
