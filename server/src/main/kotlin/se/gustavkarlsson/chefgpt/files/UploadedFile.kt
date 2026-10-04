package se.gustavkarlsson.chefgpt.files

import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.EventRepository

/** A file the user has uploaded. */
data class UploadedFile(
    val url: String,
    val mimeType: String,
    val fileName: String?,
) {
    init {
        require(url.isNotBlank()) {
            "Url must not be blank"
        }
        require(mimeType.isNotBlank()) {
            "Mime type must not be blank"
        }
    }
}

val UploadedFile.kind: FileKind? get() = fileKindOrNull(mimeType)

/**
 * The file extension the LLM clients use to label an attachment, taken from the file name when
 * there is one and falling back to the MIME subtype.
 */
val UploadedFile.format: String
    get() =
        fileName
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }
            ?: mimeType.substringAfter('/').substringBefore(';').trim()

suspend fun EventRepository.sharedAttachments(chatId: ChatId): List<UploadedFile> =
    getAll(chatId)
        .filterIsInstance<Event.Message>()
        .flatMap { it.attachments }
