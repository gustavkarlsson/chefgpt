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

    val messagesFromAi: Flow<Message> =
        flow {
            while (true) {
                val messageFromAi = session.receiveDeserialized<MessageFromAi>()
                val message = Message(Subject.Ai, messageFromAi.toMessageContent())
                emit(message)
                if (message.content.reasoning) {
                    mutableState.value = ConversationState.WaitingForAi // Keep waiting for AI if it's reasoning
                } else {
                    mutableState.value = ConversationState.WaitingForUser
                }
            }
        }
    val messagesFromUser = MutableSharedFlow<Message>()

    override val messageHistory: Flow<Message> = merge(messagesFromAi, messagesFromUser)

    private fun MessageFromAi.toMessageContent(): MessageContent =
        when (this) {
            is MessageFromAi.Regular -> MessageContent(reasoning = false, text)
            is MessageFromAi.Reasoning -> MessageContent(reasoning = true, text)
        }

    override suspend fun sayToAi(content: MessageContent) {
        mutableState.value = ConversationState.WaitingForAi
        messagesFromUser.emit(Message(Subject.User, content))
        val file = content.image
        val imageRef =
            file?.let {
                ImageRef(it.name)
            }
        session.sendSerialized(MessageFromUser(content.text, imageRef))
        if (file != null) {
            session.send(file.readBytes())
        }
    }

    override fun close() {
        mutableState.value = ConversationState.Ended
        httpClient.close()
    }
}
