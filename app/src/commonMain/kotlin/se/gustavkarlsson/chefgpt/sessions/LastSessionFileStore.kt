package se.gustavkarlsson.chefgpt.sessions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine
import kotlinx.io.writeString
import se.gustavkarlsson.chefgpt.IoOrDefault

class LastSessionFileStore(
    private val file: Path = Path("session.txt"),
) {
    suspend fun load(): SessionCredentials? =
        withContext(Dispatchers.IoOrDefault) {
            if (!SystemFileSystem.exists(file)) return@withContext null
            val source = SystemFileSystem.source(file).buffered()
            source.use {
                val usernameText = it.readLine() ?: return@withContext null
                val sessionIdText = it.readLine() ?: return@withContext null
                if (sessionIdText.isEmpty() || usernameText.isEmpty()) return@withContext null
                SessionCredentials(UserName(usernameText), SessionId(sessionIdText))
            }
        }

    suspend fun save(credentials: SessionCredentials) {
        withContext(Dispatchers.IoOrDefault) {
            clear()
            val sink = SystemFileSystem.sink(file).buffered()
            sink.use {
                it.writeString(credentials.username.value)
                it.writeString("\n")
                it.writeString(credentials.sessionId.value)
            }
        }
    }

    suspend fun clear(): Boolean =
        withContext(Dispatchers.IoOrDefault) {
            if (SystemFileSystem.exists(file)) {
                SystemFileSystem.delete(file)
                true
            } else {
                false
            }
        }
}
