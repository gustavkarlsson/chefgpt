package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import se.gustavkarlsson.chefgpt.chats.RethinkDbEventRepository
import se.gustavkarlsson.chefgpt.rethinkdb.RethinkDbAccess

fun creatEventRepositoryModule(config: ApplicationConfig) =
    module {
        single {
            val rethinkDb = getOrNull<RethinkDbAccess>()
            if (rethinkDb != null) {
                RethinkDbEventRepository(rethinkDb)
            } else {
                InMemoryEventRepository()
            }
        } bind EventRepository::class
    }
