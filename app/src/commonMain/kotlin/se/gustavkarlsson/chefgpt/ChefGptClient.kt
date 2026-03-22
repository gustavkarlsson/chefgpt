package se.gustavkarlsson.chefgpt

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.api.Event
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.UserEvent
import kotlin.uuid.Uuid

class ChefGptClient(
    private val baseUrl: String,
    username: String,
    password: String,
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
        data: ByteArray, // TODO Use a channel or similar instead.
        contentType: ContentType,
    ): ImageUrl {
        val response =
            httpClient.post("$baseUrl/images") {
                this.contentType(contentType)
                setBody(data)
            }
        return response.body()
    }

    suspend fun createChat(): Uuid {
        val response = httpClient.post("$baseUrl/chats")
        return Uuid.parse(response.body()) // TODO Can we just take the body as-is? Will it auto-parse then?
    }

    fun listenToEvents(chatId: Uuid): Flow<Event> =
        flow {
            httpClient.sse(urlString = "$baseUrl/chats/$chatId/events") {
                incoming.collect { serverSentEvent ->
                    if (serverSentEvent.event == "heartbeat") return@collect
                    val data = serverSentEvent.data ?: return@collect
                    val event = json.decodeFromString<Event>(data)
                    emit(event)
                }
            }
        }

    suspend fun sendEvent(
        chatId: Uuid,
        event: UserEvent,
    ) {
        httpClient.post("$baseUrl/chats/$chatId/events") {
            contentType(ContentType.Application.Json)
            setBody(event)
        }
    }
}
