package se.gustavkarlsson.chefgpt.chats

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine
import kotlinx.io.writeString
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.sessions.UserName
import kotlin.time.Instant

// TODO Replace with API call
data class StoredChat(
    val id: ChatId,
    val createdAt: Instant,
    val owner: UserName,
)

class ChatRepository(
    private val file: Path = Path("chats.txt"),
) {
    fun loadAll(): List<StoredChat> {
        if (!SystemFileSystem.exists(file)) return emptyList()
        val source = SystemFileSystem.source(file).buffered()
        return source.use {
            val chats = mutableListOf<StoredChat>()
            while (true) {
                val line = it.readLine() ?: break
                if (line.isBlank()) continue
                val parts = line.split("|")
                if (parts.size != 3) continue
                val chatId = ChatId.parseOrNull(parts[0]) ?: continue
                val epochSeconds = parts[1].toLongOrNull() ?: continue
                val owner = parts[2]
                chats.add(StoredChat(chatId, Instant.fromEpochSeconds(epochSeconds), UserName(owner)))
            }
            chats
        }
    }

    fun save(chat: StoredChat) {
        val existing = loadAll()
        if (SystemFileSystem.exists(file)) {
            SystemFileSystem.delete(file)
        }
        val sink = SystemFileSystem.sink(file).buffered()
        sink.use {
            for (entry in existing + chat) {
                it.writeString("${entry.id}|${entry.createdAt.epochSeconds}|${entry.owner}\n")
            }
        }
    }
}
