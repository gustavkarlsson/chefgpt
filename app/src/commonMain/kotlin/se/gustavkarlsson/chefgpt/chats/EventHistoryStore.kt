package se.gustavkarlsson.chefgpt.chats

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
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

private val log = Logger.withTag("${EventHistoryStore::class.simpleName}")

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

    suspend fun load(chatId: ChatId): Result<List<ApiEvent>, Unit> =
        withContext(Dispatchers.IoOrDefault) {
            try {
                val file = file(chatId)
                if (!SystemFileSystem.exists(file)) {
                    log.i { "No events to load" }
                    return@withContext Ok(emptyList())
                }
                SystemFileSystem.source(file).buffered().use {
                    val events = mutableListOf<ApiEvent>()
                    while (true) {
                        val line = it.readLine() ?: break
                        if (line.isBlank()) continue
                        val event = json.decodeFromString<ApiEvent>(line)
                        events.add(event)
                    }
                    log.i { "Loaded ${events.size} events" }
                    Ok(events)
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to load events" }
                Err(Unit)
            }
        }

    suspend fun append(
        chatId: ChatId,
        event: ApiEvent,
    ): Result<Unit, Unit> =
        withContext(Dispatchers.IoOrDefault) {
            try {
                load(chatId)
                    .map { existing ->
                        val file = file(chatId)
                        SystemFileSystem.delete(file, mustExist = false)
                        SystemFileSystem.sink(file).buffered().use {
                            for (entry in existing + event) {
                                it.writeString(json.encodeToString(entry))
                                it.writeString("\n")
                            }
                        }
                        log.d { "Appended event" }
                    }
            } catch (e: Exception) {
                log.e(e) { "Failed to append event" }
                Err(Unit)
            }
        }

    private fun file(chatId: ChatId): Path = Path("$dir/events_$chatId.txt")
}
