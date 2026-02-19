package se.gustavkarlsson.chefgpt

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import se.gustavkarlsson.chefgpt.api.MessageFromAi

suspend fun WebSocketServerSession.sendMessage(message: MessageToUser) {
    sendSerialized(convert(message))
}

private suspend fun WebSocketServerSession.convert(message: MessageToUser): MessageFromAi {
    return MessageFromAi.Content(message.text)
}
