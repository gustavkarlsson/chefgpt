package se.gustavkarlsson.chefgpt.setup

import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.chats.PostgresChatRepository
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess

fun createChatRepository(database: PostgresAccess?): ChatRepository =
    if (database != null) {
        PostgresChatRepository(database)
    } else {
        InMemoryChatRepository()
    }
