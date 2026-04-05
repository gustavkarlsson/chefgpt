package se.gustavkarlsson.chefgpt.auth

import app.cash.sqldelight.async.coroutines.awaitAsOne
import io.ktor.server.sessions.SessionStorage
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess

class PostgresSessionStorage(
    private val db: PostgresAccess,
) : SessionStorage {
    override suspend fun write(
        id: String,
        value: String,
    ) {
        db.use { sessionQueries.upsert(id, value) }
    }

    override suspend fun invalidate(id: String) {
        db.use { sessionQueries.deleteById(id) }
    }

    override suspend fun read(id: String): String =
        db.use {
            sessionQueries.selectById(id).awaitAsOne().value_
        }
}
