package se.gustavkarlsson.chefgpt.auth

import app.cash.sqldelight.async.coroutines.awaitAsList
import io.ktor.server.sessions.SessionStorage
import se.gustavkarlsson.chefgpt.postgres.PostgresDatabasePool
import se.gustavkarlsson.chefgpt.postgres.useSingletonScope

class PostgresSessionStorage(
    private val dbPool: PostgresDatabasePool,
) : SessionStorage {
    override suspend fun write(
        id: String,
        value: String,
    ) {
        dbPool.useSingletonScope { sessionQueries.upsert(id, value) }
    }

    override suspend fun invalidate(id: String) {
        val deletedCount = dbPool.useSingletonScope { sessionQueries.deleteById(id) }
        when (deletedCount) {
            0L -> throw NoSuchElementException("Could not invalidate session with ID: $id as it does not exist")
            1L -> Unit
            else -> error("Invalidated too many sessions with ID: $deletedCount ($deletedCount)")
        }
    }

    override suspend fun read(id: String): String {
        val sessions =
            dbPool.useSingletonScope {
                sessionQueries.selectById(id).awaitAsList()
            }
        return when (sessions.size) {
            0 -> throw NoSuchElementException("Could not find any session with ID: $id")
            1 -> sessions.first().value_
            else -> error("Invalidated too many sessions with ID: $id")
        }
    }
}
