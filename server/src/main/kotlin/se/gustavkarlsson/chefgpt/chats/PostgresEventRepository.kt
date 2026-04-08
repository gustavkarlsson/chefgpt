package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.db.SelectByChatIdAfter
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import kotlin.uuid.toJavaUuid

class PostgresEventRepository(
    private val db: DatabaseAccess,
) : EventRepository {
    private val refreshFlow = MutableSharedFlow<ChatId>(extraBufferCapacity = 64)

    override suspend fun append(
        chatId: ChatId,
        event: Event,
    ) {
        val json = Json.encodeToString<Event>(event)
        db.use {
            eventQueries.insert(chatId.value.toJavaUuid(), json)
        }
        refreshFlow.tryEmit(chatId)
    }

    override suspend fun getAll(chatId: ChatId): List<Event> =
        db.use {
            eventQueries
                .selectByChatIdAfter(chatId.value.toJavaUuid(), 0L)
                .executeAsList()
                .map { row -> row.parseEvent() }
        }

    override fun flow(
        chatId: ChatId,
        last: EventId?,
    ): Flow<Event> =
        channelFlow {
            var lastRowId =
                if (last != null) {
                    db.use {
                        eventQueries
                            .findRowId(chatId.value.toJavaUuid(), last.value.toString())
                            .executeAsOneOrNull()
                    } ?: 0L
                } else {
                    0L
                }
            val triggers =
                refreshFlow
                    .filter { it == chatId }
                    .onStart { emit(chatId) }
            triggers.collectLatest {
                val rows =
                    db.use {
                        eventQueries
                            .selectByChatIdAfter(chatId.value.toJavaUuid(), lastRowId)
                            .executeAsList()
                    }
                for (row in rows) {
                    send(row.parseEvent())
                    lastRowId = row.id
                }
            }
        }
}

private fun SelectByChatIdAfter.parseEvent(): Event = Json.decodeFromString(json)
