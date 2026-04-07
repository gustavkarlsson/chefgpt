package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import se.gustavkarlsson.chefgpt.chats.PostgresEventRepository
import se.gustavkarlsson.chefgpt.postgres.PostgresDatabasePool

fun Application.createEventRepositoryModule() =
    module {
        single {
            val dbPool = getOrNull<PostgresDatabasePool>()
            if (dbPool != null) {
                PostgresEventRepository(dbPool)
            } else {
                InMemoryEventRepository()
            }
        } bind EventRepository::class
    }
