package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface ConversationFactory {
    fun create(
        sessionId: SessionId,
        chatId: ChatId,
    ): Conversation
}
