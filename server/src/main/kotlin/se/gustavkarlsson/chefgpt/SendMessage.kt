package se.gustavkarlsson.chefgpt

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import se.gustavkarlsson.chefgpt.api.ApiAgentMessage

suspend fun WebSocketServerSession.sendMessage(message: AgentMessage) {
    sendSerialized(convert(message))
}

private suspend fun WebSocketServerSession.convert(message: AgentMessage): ApiAgentMessage =
    when (message) {
        is AgentMessage.Reasoning -> ApiAgentMessage.Reasoning(message.text)
        is AgentMessage.Regular -> ApiAgentMessage.Regular(message.text)
    }
