package se.gustavkarlsson.chefgpt

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine
import kotlinx.io.writeString

data class Credentials(
    val username: String,
    val password: String,
)

class LoginRepository(
    private val file: Path,
) {
    fun load(): Credentials? {
        if (!SystemFileSystem.exists(file)) return null
        val source = SystemFileSystem.source(file).buffered()
        return source.use {
            val username = it.readLine() ?: return null
            val password = it.readLine() ?: return null
            Credentials(username, password)
        }
    }

    fun save(credentials: Credentials) {
        clear()
        val sink = SystemFileSystem.sink(file).buffered()
        sink.use {
            it.writeString(credentials.username + "\n" + credentials.password + "\n")
        }
    }

    fun clear() {
        if (SystemFileSystem.exists(file)) {
            SystemFileSystem.delete(file)
        }
    }
}
