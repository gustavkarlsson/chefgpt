package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.SessionStorageMemory
import se.gustavkarlsson.chefgpt.auth.PostgresSessionStorage
import se.gustavkarlsson.chefgpt.db.DatabaseAccess

fun createSessionStorage(database: DatabaseAccess?): SessionStorage =
    if (database != null) {
        PostgresSessionStorage(database)
    } else {
        SessionStorageMemory()
    }
