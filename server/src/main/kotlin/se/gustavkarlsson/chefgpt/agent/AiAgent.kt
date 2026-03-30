package se.gustavkarlsson.chefgpt.agent

import io.ktor.server.routing.RoutingContext
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId

interface AiAgent {
    // TODO Replace with context receiver when stable
    suspend fun RoutingContext.run(
        userId: UserId,
        chatId: ChatId,
    )
}
