package se.gustavkarlsson.chefgpt.setup

import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.chats.PostgresChatRepository
import se.gustavkarlsson.chefgpt.db.DatabaseAccess

fun createChatRepository(database: DatabaseAccess?): ChatRepository =
    if (database != null) {
        PostgresChatRepository(database)
    } else {
        InMemoryChatRepository()
    }
