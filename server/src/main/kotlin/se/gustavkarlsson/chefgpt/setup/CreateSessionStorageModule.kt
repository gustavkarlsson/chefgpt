package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.SessionStorageMemory
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.auth.PostgresSessionStorage
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess

fun Application.createSessionStorageModule() =
    module {
        single {
            val db = getOrNull<DatabaseAccess>()
            if (db != null) {
                PostgresSessionStorage(db)
            } else {
                SessionStorageMemory()
            }
        } bind SessionStorage::class
    }
