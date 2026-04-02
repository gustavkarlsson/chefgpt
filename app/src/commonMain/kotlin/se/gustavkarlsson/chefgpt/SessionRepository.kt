package se.gustavkarlsson.chefgpt

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine
import kotlinx.io.writeString

data class SessionCredentials(
    val username: String,
    val sessionId: SessionId,
)

class SessionRepository(
    private val file: Path = Path("sessions.txt"),
) {
    fun load(): SessionCredentials? {
        if (!SystemFileSystem.exists(file)) return null
        val source = SystemFileSystem.source(file).buffered()
        return source.use {
            val username = it.readLine() ?: return null
            val sessionIdText = it.readLine() ?: return null
            if (sessionIdText.isEmpty() || username.isEmpty()) return null
            SessionCredentials(username, SessionId(sessionIdText))
        }
    }

    fun save(credentials: SessionCredentials) {
        clear()
        val sink = SystemFileSystem.sink(file).buffered()
        sink.use {
            it.writeString(credentials.username)
            it.writeString("\n")
            it.writeString(credentials.sessionId.toString())
        }
    }

    fun clear() {
        if (SystemFileSystem.exists(file)) {
            SystemFileSystem.delete(file)
        }
    }
}
