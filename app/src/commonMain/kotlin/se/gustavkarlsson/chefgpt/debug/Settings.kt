package se.gustavkarlsson.chefgpt.debug

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import se.gustavkarlsson.chefgpt.APP_STORAGE_DIR
import se.gustavkarlsson.chefgpt.IoOrDefault
import se.gustavkarlsson.chefgpt.SERVER_BASE_URL

private const val KEY_BASE_URL = "base_url"

private val log = Logger.withTag("${Settings::class.simpleName}")

// Holds developer-only settings, persisted as key=value lines in debug.conf.
// The file is read once — lazily, on first access — into an in-memory cache.
// Thereafter reads hit the cache (effectively non-blocking) and writes update
// the cache and then the file; the file is never read again. File access is
// blocking and therefore confined to the injected I/O dispatcher.
class Settings(
    private val file: Path = Path("$APP_STORAGE_DIR/debug.conf"),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IoOrDefault,
) {
    private val mutex = Mutex()
    private var cache: MutableMap<String, String>? = null

    // Effective base URL: the override if set, otherwise the platform default.
    suspend fun getBaseUrl(): String = get(KEY_BASE_URL) ?: SERVER_BASE_URL

    // A blank value clears the override, falling back to the platform default.
    suspend fun setBaseUrl(value: String) = set(KEY_BASE_URL, value.trim().ifBlank { null })

    private suspend fun get(key: String): String? = mutex.withLock { getOrCreateCache()[key] }

    private suspend fun set(
        key: String,
        value: String?,
    ) = mutex.withLock {
        val cache = getOrCreateCache()
        if (value == null) cache.remove(key) else cache[key] = value
        withContext(dispatcher) { writeFileBlocking(cache) }
    }

    // Returns the cache, loading it from disk on first access. Must be called while holding [mutex].
    private suspend fun getOrCreateCache(): MutableMap<String, String> =
        cache ?: withContext(dispatcher) { readFileBlocking() }.toMutableMap().also { cache = it }

    private fun readFileBlocking(): Map<String, String> {
        return try {
            if (!SystemFileSystem.exists(file)) return emptyMap()
            val text = SystemFileSystem.source(file).buffered().use { it.readString() }
            text
                .lines()
                .map { it.trim() }
                .filterNot { it.isEmpty() }
                .mapNotNull { line ->
                    val index = line.indexOf('=')
                    if (index <= 0) null else line.substring(0, index) to line.substring(index + 1)
                }.toMap()
        } catch (e: Exception) {
            log.e(e) { "Failed to read debug config from $file" }
            emptyMap()
        }
    }

    private fun writeFileBlocking(entries: Map<String, String>) {
        try {
            SystemFileSystem.sink(file).buffered().use { sink ->
                entries.forEach { (key, value) -> sink.writeString("$key=$value\n") }
            }
            log.i { "Saved debug config to $file" }
        } catch (e: Exception) {
            log.e(e) { "Failed to save debug config to $file" }
        }
    }
}
