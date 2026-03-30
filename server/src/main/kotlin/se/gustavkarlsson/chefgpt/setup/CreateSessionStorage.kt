package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.SessionStorageMemory
import se.gustavkarlsson.chefgpt.auth.PostgresSessionStorage
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase

fun createSessionStorage(database: ChefGptDatabase?): SessionStorage =
    if (database != null) {
        PostgresSessionStorage(database.sessionQueries)
    } else {
        SessionStorageMemory()
    }
