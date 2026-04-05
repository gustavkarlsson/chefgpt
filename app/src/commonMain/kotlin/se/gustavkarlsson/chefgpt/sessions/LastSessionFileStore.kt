package se.gustavkarlsson.chefgpt.sessions

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.ktor.utils.io.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import se.gustavkarlsson.chefgpt.IoOrDefault

private val log = Logger.withTag("${LastSessionFileStore::class.simpleName}")

class LastSessionFileStore(
    private val file: Path = Path("session.txt"),
) {
    suspend fun load(): Result<SessionCredentials?, Unit> =
        withContext(Dispatchers.IoOrDefault) {
            val text =
                try {
                    if (!SystemFileSystem.exists(file)) {
                        log.i { "No last session found in $file" }
                        return@withContext Ok(null)
                    }
                    SystemFileSystem.source(file).buffered().use { it.readText() }
                } catch (e: Exception) {
                    log.e(e) { "Failed to read last session from $file" }
                    return@withContext Err(Unit)
                }
            val lines =
                text
                    .lines()
                    .map { it.trim() }
                    .filterNot { it.isEmpty() }
            if (lines.size != 2) {
                log.e { "Invalid format for $file" }
            }
            val usernameText = lines[0]
            val sessionIdText = lines[1]
            log.i { "Read last session ($usernameText) from $file" }
            Ok(SessionCredentials(UserName(usernameText), SessionId(sessionIdText)))
        }

    suspend fun save(credentials: SessionCredentials): Boolean =
        withContext(Dispatchers.IoOrDefault) {
            try {
                clear()
                SystemFileSystem.sink(file).buffered().use { sink ->
                    sink.writeString(credentials.username.value)
                    sink.writeString("\n")
                    sink.writeString(credentials.sessionId.value)
                }
                log.i { "Saved session for ${credentials.username} to $file" }
                true
            } catch (e: Exception) {
                log.e(e) { "Failed to save session to $file" }
                false
            }
        }

    suspend fun clear(): Boolean =
        withContext(Dispatchers.IoOrDefault) {
            try {
                if (SystemFileSystem.exists(file)) {
                    SystemFileSystem.delete(file, mustExist = false)
                    log.i { "Cleared last session from $file" }
                    true
                } else {
                    log.w { "No last session to clear from $file" }
                    false
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to clear last session from $file" }
                false
            }
        }
}
