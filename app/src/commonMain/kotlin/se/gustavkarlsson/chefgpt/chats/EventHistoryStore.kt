package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine
import kotlinx.io.writeString
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.IoOrDefault
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ChatId

// TODO Replace with database
class EventHistoryStore(
    private val dir: Path = Path("."),
    private val prettyPrint: Boolean = false,
) {
    private val json =
        Json {
            prettyPrint = this@EventHistoryStore.prettyPrint
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }

    // TODO Return Result
    suspend fun load(chatId: ChatId): List<ApiEvent> =
        withContext(Dispatchers.IoOrDefault) {
            val file = file(chatId)
            if (!SystemFileSystem.exists(file)) return@withContext emptyList()
            val source = SystemFileSystem.source(file).buffered()
            source.use {
                val events = mutableListOf<ApiEvent>()
                while (true) {
                    val line = it.readLine() ?: break
                    if (line.isBlank()) continue
                    val event = runCatching { json.decodeFromString<ApiEvent>(line) }.getOrNull() ?: continue
                    events.add(event)
                }
                events
            }
        }

    // TODO Return Result
    suspend fun append(
        chatId: ChatId,
        event: ApiEvent,
    ) {
        withContext(Dispatchers.IoOrDefault) {
            val file = file(chatId)
            val existing = load(chatId)
            if (SystemFileSystem.exists(file)) {
                SystemFileSystem.delete(file)
            }
            val sink = SystemFileSystem.sink(file).buffered()
            sink.use {
                for (entry in existing + event) {
                    it.writeString(json.encodeToString(entry))
                    it.writeString("\n")
                }
            }
        }
    }

    private fun file(chatId: ChatId): Path = Path("$dir/events_$chatId.txt")
}
