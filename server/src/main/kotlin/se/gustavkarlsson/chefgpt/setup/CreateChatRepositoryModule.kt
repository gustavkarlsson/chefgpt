package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.chats.PostgresChatRepository
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess

fun createChatRepositoryModule(config: ApplicationConfig) =
    module {
        single {
            val database = getOrNull<PostgresAccess>()
            if (database != null) {
                PostgresChatRepository(database)
            } else {
                InMemoryChatRepository()
            }
        } bind ChatRepository::class
    }
