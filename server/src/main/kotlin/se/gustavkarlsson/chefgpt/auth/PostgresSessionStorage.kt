package se.gustavkarlsson.chefgpt.auth

import io.ktor.server.sessions.SessionStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import se.gustavkarlsson.chefgpt.db.withTransaction

private object SessionTable : IdTable<String>("session") {
    override val id: Column<EntityID<String>> = text("id").entityId()
    override val primaryKey = PrimaryKey(id)
    val value = text("value")
}

class PostgresSessionStorage(
    private val db: R2dbcDatabase,
) : SessionStorage {
    override suspend fun write(
        id: String,
        value: String,
    ) {
        db.withTransaction {
            SessionTable.insert {
                it[this.id] = id
                it[this.value] = value
            }
        }
    }

    override suspend fun invalidate(id: String) {
        db.withTransaction {
            SessionTable.deleteWhere { this.id eq id }
        }
    }

    override suspend fun read(id: String): String =
        db.withTransaction {
            SessionTable
                .select(SessionTable.value)
                .where { SessionTable.id eq id }
                .limit(1)
                .map { it[SessionTable.value] }
                .first()
        }
}
