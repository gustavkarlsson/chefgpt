package se.gustavkarlsson.chefgpt.chats

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.db.SelectByChatIdAfter
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess
import kotlin.uuid.toJavaUuid

class PostgresEventRepository(
    private val db: PostgresAccess,
) : EventRepository {
    private val refreshFlow = MutableSharedFlow<ChatId>(extraBufferCapacity = 64)

    override suspend fun append(
        chatId: ChatId,
        event: Event,
    ) {
        val json = Json.encodeToString<Event>(event)
        db.use {
            eventQueries.insert(
                chat_id = chatId.value.toJavaUuid(),
                value = json,
            )
        }
        refreshFlow.tryEmit(chatId)
    }

    override suspend fun getAll(chatId: ChatId): List<Event> =
        db.use {
            eventQueries
                .selectByChatIdAfter(chatId.value.toJavaUuid(), 0L)
                .awaitAsList()
                .map { row -> row.parseEvent() }
        }

    override fun flow(
        chatId: ChatId,
        last: EventId?,
    ): Flow<Event> =
        flow {
            var lastRowId =
                if (last != null) {
                    db.use {
                        eventQueries
                            .findRowId(last.value.toString())
                            .awaitAsOneOrNull()
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
                            .awaitAsList()
                    }
                for (row in rows) {
                    emit(row.parseEvent())
                    lastRowId = row.id
                }
            }
        }
}

private fun SelectByChatIdAfter.parseEvent(): Event = Json.decodeFromString(json)
