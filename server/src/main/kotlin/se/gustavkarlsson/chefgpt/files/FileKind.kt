package se.gustavkarlsson.chefgpt.files

import io.ktor.http.ContentType
import se.gustavkarlsson.chefgpt.api.ApiUploadedFile

/**
 * The kinds of files the agent can actually read. Anything else is rejected on upload,
 * so a user never gets a file into a chat that the agent has to silently ignore.
 */
enum class FileKind {
    Image,
    Pdf,
    Text,
}

fun fileKindOrNull(mimeType: String): FileKind? =
    when {
        mimeType.startsWith("image/") -> FileKind.Image
        mimeType.substringBefore(';').trim() == "application/pdf" -> FileKind.Pdf
        mimeType.startsWith("text/") -> FileKind.Text
        else -> null
    }

fun fileKindOrNull(contentType: ContentType?): FileKind? =
    contentType?.let { fileKindOrNull("${it.contentType}/${it.contentSubtype}") }

val ApiUploadedFile.kind: FileKind? get() = fileKindOrNull(mimeType)

/**
 * The file extension the LLM clients use to label an attachment, taken from the file name when
 * there is one and falling back to the MIME subtype.
 */
val ApiUploadedFile.format: String
    get() =
        fileName
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }
            ?: mimeType.substringAfter('/').substringBefore(';').trim()
