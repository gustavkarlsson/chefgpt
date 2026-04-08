package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.db.SelectByChatIdAfter
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.util.RepoSyncer
import kotlin.uuid.toJavaUuid

class PostgresEventRepository(
    private val db: DatabaseAccess,
) : EventRepository {
    private val syncer = RepoSyncer<ChatId>()

    override suspend fun append(
        chatId: ChatId,
        event: Event,
    ) {
        val json = Json.encodeToString<Event>(event)
        db.use {
            eventQueries.insert(chatId.value.toJavaUuid(), json)
        }
        syncer.notifyChange(chatId)
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
            syncer.listen(chatId).collect {
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
