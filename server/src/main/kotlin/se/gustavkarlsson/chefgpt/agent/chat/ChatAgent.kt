package se.gustavkarlsson.chefgpt.agent.chat

import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId

interface ChatAgent {
    suspend fun run(
        userId: UserId,
        chatId: ChatId,
    )
}
