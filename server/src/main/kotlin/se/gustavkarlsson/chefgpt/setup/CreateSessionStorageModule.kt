package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.SessionStorageMemory
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.auth.PostgresSessionStorage
import se.gustavkarlsson.chefgpt.postgres.PostgresDatabasePool

fun Application.createSessionStorageModule() =
    module {
        single {
            val dbPool = getOrNull<PostgresDatabasePool>()
            if (dbPool != null) {
                PostgresSessionStorage(dbPool)
            } else {
                SessionStorageMemory()
            }
        } bind SessionStorage::class
    }
