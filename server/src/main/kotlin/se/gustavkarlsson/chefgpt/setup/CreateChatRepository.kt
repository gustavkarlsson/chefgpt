package se.gustavkarlsson.chefgpt.setup

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.chats.PostgresChatRepository

fun createChatRepository(database: R2dbcDatabase?): ChatRepository =
    if (database != null) {
        PostgresChatRepository(database)
    } else {
        InMemoryChatRepository()
    }
