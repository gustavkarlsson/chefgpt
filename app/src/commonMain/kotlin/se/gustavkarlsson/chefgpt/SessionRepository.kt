package se.gustavkarlsson.chefgpt

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import se.gustavkarlsson.chefgpt.api.SessionId

class SessionRepository(
    private val file: Path,
) {
    fun load(): SessionId? {
        if (!SystemFileSystem.exists(file)) return null
        val source = SystemFileSystem.source(file).buffered()
        return source.use {
            val text = it.readString().trim()
            if (text.isEmpty()) return null
            SessionId.parse(text)
        }
    }

    fun save(sessionId: SessionId) {
        clear()
        val sink = SystemFileSystem.sink(file).buffered()
        sink.use {
            it.writeString(sessionId.toString())
        }
    }

    fun clear() {
        if (SystemFileSystem.exists(file)) {
            SystemFileSystem.delete(file)
        }
    }
}
