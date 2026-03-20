package se.gustavkarlsson.chefgpt

import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.gustavkarlsson.chefgpt.api.FileId
import se.gustavkarlsson.chefgpt.chats.ChatId
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

class ImageStore(
    private val storageDir: Path,
) {
    suspend fun writeFile(
        chatId: ChatId,
        fileId: FileId,
        readChannel: ByteReadChannel,
    ): Path =
        withContext(Dispatchers.IO) {
            val file = getFilePath(chatId, fileId)
            Files.createDirectories(file.parent)
            readChannel.copyAndClose(file.toFile().writeChannel())
            file
        }

    suspend fun getFile(
        chatId: ChatId,
        fileId: FileId,
    ): Path? =
        withContext(Dispatchers.IO) {
            getFilePath(chatId, fileId).takeIf { it.exists() }
        }

    suspend fun deleteFile(
        chatId: ChatId,
        fileId: FileId,
    ): Boolean =
        withContext(Dispatchers.IO) {
            getFilePath(chatId, fileId).deleteIfExists()
        }

    private fun getFilePath(
        chatId: ChatId,
        fileId: FileId,
    ): Path {
        val sessionDir = storageDir.resolve(chatId.value.toString())
        return sessionDir.resolve(fileId.value.toString())
    }
}
