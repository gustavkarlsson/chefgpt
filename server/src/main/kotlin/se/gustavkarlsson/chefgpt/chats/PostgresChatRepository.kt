package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.db.withTransaction

private object Table : UuidTable("chat") {
    val userId = uuid("user_id")
}

class PostgresChatRepository(
    private val db: R2dbcDatabase,
) : ChatRepository {
    override suspend fun create(userId: UserId): Chat =
        db.withTransaction {
            val id =
                Table
                    .insert {
                        it[Table.userId] = userId.value
                    }.get(Table.id)
                    .value
            Chat(ChatId(id))
        }

    override suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        db.withTransaction {
            Table.deleteWhere { id eq chatId.value } > 0
        }

    override suspend fun getAll(userId: UserId): List<Chat> =
        db.withTransaction {
            Table
                .selectAll()
                .map { row ->
                    val id = ChatId(row[Table.id].value)
                    Chat(id)
                }.toList()
        }

    override suspend fun get(
        userId: UserId,
        chatId: ChatId,
    ): Chat? =
        db.withTransaction {
            Table
                .selectAll()
                .where { (Table.userId eq userId.value) and (Table.id eq chatId.value) }
                .limit(1)
                .map { row ->
                    val id = ChatId(row[Table.id].value)
                    Chat(id)
                }.firstOrNull()
        }

    override suspend fun get(chatId: ChatId): Chat? =
        db.withTransaction {
            Table
                .selectAll()
                .where { Table.id eq chatId.value }
                .limit(1)
                .map { row ->
                    val id = ChatId(row[Table.id].value)
                    Chat(id)
                }.firstOrNull()
        }

    override suspend fun contains(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        db.withTransaction {
            Table
                .selectAll()
                .where { (Table.userId eq userId.value) and (Table.id eq chatId.value) }
                .limit(1)
                .empty()
                .not()
        }
}
