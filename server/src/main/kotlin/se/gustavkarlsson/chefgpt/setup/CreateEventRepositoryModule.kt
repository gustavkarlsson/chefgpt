package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import se.gustavkarlsson.chefgpt.chats.PostgresEventRepository
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess

fun Application.createEventRepositoryModule() =
    module {
        single {
            val postgres = getOrNull<PostgresAccess>()
            if (postgres != null) {
                PostgresEventRepository(postgres)
            } else {
                InMemoryEventRepository()
            }
        } bind EventRepository::class
    }
