package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.SessionStorageMemory
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import se.gustavkarlsson.chefgpt.auth.PostgresSessionStorage

fun createSessionStorage(database: R2dbcDatabase?): SessionStorage =
    if (database != null) {
        PostgresSessionStorage(database)
    } else {
        SessionStorageMemory()
    }
