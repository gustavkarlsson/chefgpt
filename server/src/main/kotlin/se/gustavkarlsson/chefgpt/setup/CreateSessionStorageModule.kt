package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.SessionStorageMemory
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.auth.PostgresSessionStorage
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess

fun createSessionStorageModule(config: ApplicationConfig) =
    module {
        single {
            val database = getOrNull<PostgresAccess>()
            if (database != null) {
                PostgresSessionStorage(database)
            } else {
                SessionStorageMemory()
            }
        } bind SessionStorage::class
    }
