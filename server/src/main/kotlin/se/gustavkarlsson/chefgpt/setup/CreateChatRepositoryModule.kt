package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.chats.PostgresChatRepository
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess

fun Application.createChatRepositoryModule() =
    module {
        single {
            val db = getOrNull<DatabaseAccess>()
            if (db != null) {
                PostgresChatRepository(db)
            } else {
                InMemoryChatRepository()
            }
        } bind ChatRepository::class
    }
