package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.chats.PostgresChatRepository
import se.gustavkarlsson.chefgpt.postgres.PostgresDatabasePool

fun Application.createChatRepositoryModule() =
    module {
        single {
            val dbPool = getOrNull<PostgresDatabasePool>()
            if (dbPool != null) {
                PostgresChatRepository(dbPool)
            } else {
                InMemoryChatRepository()
            }
        } bind ChatRepository::class
    }
