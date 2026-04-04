package se.gustavkarlsson.chefgpt.agent

import io.ktor.server.routing.RoutingContext
import se.gustavkarlsson.chefgpt.api.ChatId

interface AiAgent {
    // TODO Consider replacing with context receiver
    suspend fun RoutingContext.run(chatId: ChatId)
}
