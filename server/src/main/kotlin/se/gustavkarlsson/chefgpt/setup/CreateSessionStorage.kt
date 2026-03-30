package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.SessionStorageMemory
import se.gustavkarlsson.chefgpt.auth.PostgresSessionStorage
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess

fun createSessionStorage(database: PostgresAccess?): SessionStorage =
    if (database != null) {
        PostgresSessionStorage(database)
    } else {
        SessionStorageMemory()
    }
