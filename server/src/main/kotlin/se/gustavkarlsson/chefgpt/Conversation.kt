package se.gustavkarlsson.chefgpt

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized

class Conversation(private val session: WebSocketServerSession) {
    suspend fun send(message: MessageFromAi) {
        session.sendSerialized(message)
    }
    suspend fun await(): MessageFromUser {
        return session.receiveDeserialized()
    }
}
