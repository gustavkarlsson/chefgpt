package se.gustavkarlsson.chefgpt.chats

import com.rethinkdb.net.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.rethinkdb.RethinkDbAccess
import kotlin.uuid.Uuid

@Suppress("UNCHECKED_CAST")
class RethinkDbEventRepository(
    private val rethinkDb: RethinkDbAccess,
) : EventRepository {
    override suspend fun append(
        chatId: ChatId,
        event: Event,
    ) {
        val jsonString = Json.encodeToString<Event>(event)
        rethinkDb.use { r, conn ->
            r
                .table("events")
                .insert(
                    r
                        .hashMap("id", Uuid.generateV7().toString())
                        .with("chat_id", chatId.value.toString())
                        .with("json", jsonString),
                ).run(conn)
        }
    }

    override suspend fun getAll(chatId: ChatId): List<Event> =
        rethinkDb.use { r, conn ->
            r
                .table("events")
                .getAll(chatId.value.toString())
                .optArg("index", "chat_id")
                .run(conn)
                .use { result ->
                    result
                        .map { row ->
                            val map = row as Map<String, Any>
                            Json.decodeFromString<Event>(map["json"] as String)
                        }.sortedBy { it.id.value }
                }
        }

    override fun flow(
        chatId: ChatId,
        last: EventId?,
    ): Flow<Event> =
        callbackFlow {
            val conn = rethinkDb.createConnection()
            val result: Result<Any> =
                rethinkDb.r
                    .table("events")
                    .getAll(chatId.value.toString())
                    .optArg("index", "chat_id")
                    .changes()
                    .optArg("include_initial", true)
                    .run(conn)

            launch(Dispatchers.IO) {
                try {
                    for (change in result) {
                        val changeMap = change as Map<String, Any>
                        val newVal = changeMap["new_val"] as? Map<String, Any> ?: continue
                        val jsonString = newVal["json"] as? String ?: continue
                        val event = Json.decodeFromString<Event>(jsonString)
                        send(event)
                    }
                } finally {
                    result.close()
                    conn.close()
                }
            }

            awaitClose {
                result.close()
                conn.close()
            }
        }.let { flow ->
            if (last == null) {
                flow
            } else {
                flow.dropWhile { event -> event.id != last }.drop(1)
            }
        }
}
