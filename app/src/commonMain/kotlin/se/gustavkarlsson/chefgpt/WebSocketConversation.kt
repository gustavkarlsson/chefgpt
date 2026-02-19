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
import io.ktor.websocket.close
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.api.MessageFromAi
import se.gustavkarlsson.chefgpt.api.MessageFromUser

private val httpClient: HttpClient =
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

suspend fun startWebSocketConversation(
    host: String = "localhost",
    port: Int = DEV_SERVER_PORT,
    path: String = "/find-recipe-chat",
): UserSideConversation {
    val session = httpClient.webSocketSession(host = host, port = port, path = path)
    return HttpClientConversation(session)
}

private class HttpClientConversation(private val session: DefaultClientWebSocketSession) : UserSideConversation {
    private val mutableState = MutableStateFlow(ConversationState.WaitingForAi)
    override val state: StateFlow<ConversationState> = mutableState.asStateFlow()

    val messagesFromAi: Flow<Message> = flow {
        while (true) {
            val messageFromAi = session.receiveDeserialized<MessageFromAi>()
            val message = Message(Subject.Ai, messageFromAi.toMessageContent())
            emit(message)
            mutableState.value = ConversationState.WaitingForUser
        }
    }
    val messagesFromUser = MutableSharedFlow<Message>()

    override val messageHistory: Flow<Message> = merge(messagesFromAi, messagesFromUser)

    private fun MessageFromAi.toMessageContent(): MessageContent {
        return when (this) {
            is MessageFromAi.Content -> MessageContent(text)
        }
    }

    override suspend fun sayToAi(content: MessageContent) {
        mutableState.value = ConversationState.WaitingForAi
        messagesFromUser.emit(Message(Subject.User, content))
        session.sendSerialized(MessageFromUser(content.text))
    }

    override fun close() {
        // FIXME don't close with global scope
        mutableState.value = ConversationState.Ended
        GlobalScope.launch { session.close() }
    }

}
