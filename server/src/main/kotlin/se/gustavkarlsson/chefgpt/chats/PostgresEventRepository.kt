package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.json.json
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.db.withTransaction

private object EventTable : UuidTable("event") {
    val chatId = uuid("chat_id")
    val json = json<Event>("json", Json)
}

class PostgresEventRepository(
    private val db: R2dbcDatabase,
) : EventRepository {
    override suspend fun append(
        chatId: ChatId,
        event: Event,
    ) {
        db.withTransaction {
            EventTable.insert {
                it[this.chatId] = chatId.value
                it[json] = event
            }
        }
    }

    override suspend fun getAll(chatId: ChatId): List<Event> =
        db.withTransaction {
            EventTable
                .selectAll()
                .where { EventTable.chatId eq chatId.value }
                .map { it[EventTable.json] }
                .toList()
        }

    override fun flow(
        chatId: ChatId,
        last: EventId?,
    ): Flow<Event> =
        EventTable
            .selectAll()
            .where {
                if (last == null) {
                    EventTable.chatId eq chatId.value
                } else {
                    (EventTable.chatId eq chatId.value) and (EventTable.id greater last.value)
                }
            }.map { it[EventTable.json] }
            .onCompletion {
                error("Flow completed unexpectedly")
            }
}
