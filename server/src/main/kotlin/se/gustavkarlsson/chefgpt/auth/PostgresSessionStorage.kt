package se.gustavkarlsson.chefgpt.auth

import io.ktor.server.sessions.SessionStorage
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess

class PostgresSessionStorage(
    private val db: DatabaseAccess,
) : SessionStorage {
    override suspend fun write(
        id: String,
        value: String,
    ) {
        db.use { sessionQueries.upsert(id, value) }
    }

    override suspend fun invalidate(id: String) {
        val deletedCount = db.use { sessionQueries.deleteById(id).value }
        when (deletedCount) {
            0L -> throw NoSuchElementException("Could not invalidate session with ID: $id as it does not exist")
            1L -> Unit
            else -> error("Invalidated too many sessions with ID: $id ($deletedCount)")
        }
    }

    override suspend fun read(id: String): String {
        val sessions =
            db.use {
                sessionQueries.selectById(id).executeAsList()
            }
        return when (sessions.size) {
            0 -> throw NoSuchElementException("Could not find any session with ID: $id")
            1 -> sessions.first().value_
            else -> error("Invalidated too many sessions with ID: $id")
        }
    }
}
