package se.gustavkarlsson.chefgpt

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.send
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.api.ImageRef
import se.gustavkarlsson.chefgpt.api.MessageFromAi
import se.gustavkarlsson.chefgpt.api.MessageFromUser

suspend fun startWebSocketConversation(
    host: String = "localhost",
    port: Int = DEV_SERVER_PORT,
    path: String = "/find-recipe-chat",
): Conversation {
    val httpClient =
        HttpClient(CIO) {
            // FIXME configure JSON reasonably
            val jsonConfig = Json
            install(ContentNegotiation) {
                json(jsonConfig)
            }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(jsonConfig)
            }
        }
    val session = httpClient.webSocketSession(host = host, port = port, path = path)
    return HttpClientConversation(httpClient, session)
}

private class HttpClientConversation(
    private val httpClient: HttpClient,
    private val session: DefaultClientWebSocketSession,
) : Conversation,
    AutoCloseable {
    private val mutableState = MutableStateFlow(ConversationState.WaitingForAi)
    override val state: StateFlow<ConversationState> = mutableState.asStateFlow()

    val messagesFromAi: Flow<AiMessage> =
        flow {
            while (true) {
                val message =
                    when (val messageFromAi = session.receiveDeserialized<MessageFromAi>()) {
                        is MessageFromAi.Regular -> AiMessage.Regular(messageFromAi.text)
                        is MessageFromAi.Reasoning -> AiMessage.Reasoning(messageFromAi.text)
                    }
                emit(message)
                mutableState.value =
                    when (message) {
                        is AiMessage.Regular -> ConversationState.WaitingForUser
                        is AiMessage.Reasoning -> ConversationState.WaitingForAi // Keep waiting if AI is reasoning
                    }
            }
        }
    val messagesFromUser = MutableSharedFlow<UserMessage>()

    override val messageHistory: Flow<Message> = merge(messagesFromAi, messagesFromUser)

    override suspend fun sayToAi(message: UserMessage) {
        mutableState.value = ConversationState.WaitingForAi
        messagesFromUser.emit(UserMessage(message.text, message.image))
        val file = message.image
        val imageRef =
            file?.let {
                ImageRef(it.name)
            }
        session.sendSerialized(MessageFromUser(message.text, imageRef))
        if (file != null) {
            session.send(file.readBytes())
        }
    }

    override fun close() {
        mutableState.value = ConversationState.Ended
        httpClient.close()
    }
}
