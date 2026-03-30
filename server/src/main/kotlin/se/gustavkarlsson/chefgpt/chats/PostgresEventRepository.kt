package se.gustavkarlsson.chefgpt.chats

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.db.DatabaseAccess
import kotlin.uuid.toJavaUuid
import java.util.UUID as JavaUUID

class PostgresEventRepository(
    private val db: DatabaseAccess,
) : EventRepository {
    override suspend fun append(
        chatId: ChatId,
        event: Event,
    ) {
        // TODO Consider injecting a configured Json instance
        val jsonString = Json.encodeToString<Event>(event)
        db.use {
            eventQueries.insert(
                chat_id = chatId.value.toJavaUuid(),
                json = jsonString,
            )
        }
    }

    override suspend fun getAll(chatId: ChatId): List<Event> = db.use {
        eventQueries
            .selectByChatId(chatId.value.toJavaUuid())
            .awaitAsList()
            // TODO Consider injecting a configured Json instance
            .map { row -> Json.decodeFromString<Event>(row.json) }
    }

    // TODO This polls the full result set on every change, which is not very efficient.
    //  Consider using a database better suited for streaming, such as an event store.
    override fun flow(
        chatId: ChatId,
        last: EventId?,
    ): Flow<Event> = db.stream {
        val query =
            if (last == null) {
                eventQueries.selectByChatId(chatId.value.toJavaUuid())
            } else {
                eventQueries.selectByChatIdAfterId(
                    chatId.value.toJavaUuid(),
                    last.value.toJavaUuid(),
                )
            }
        flow {
            val emittedIds = mutableSetOf<JavaUUID>()
            query
                .asFlow()
                .mapToList(Dispatchers.IO)
                .map { rows -> rows.filter { it.id !in emittedIds } }
                .collect { rows ->
                    emittedIds.addAll(rows.map { it.id })
                    for (row in rows) {
                        emit(row)
                    }
                }
        }.map { row ->
            Json.decodeFromString<Event>(row.json)
        }
    }
}
