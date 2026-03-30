package se.gustavkarlsson.chefgpt.auth

import app.cash.sqldelight.async.coroutines.awaitAsOne
import io.ktor.server.sessions.SessionStorage
import se.gustavkarlsson.chefgpt.db.SessionQueries

class PostgresSessionStorage(
    private val sessionQueries: SessionQueries,
) : SessionStorage {
    override suspend fun write(
        id: String,
        value: String,
    ) {
        sessionQueries.insert(id, value)
    }

    override suspend fun invalidate(id: String) {
        sessionQueries.deleteById(id)
    }

    override suspend fun read(id: String): String = sessionQueries.selectById(id).awaitAsOne().value_
}
