package se.gustavkarlsson.chefgpt.files

import io.ktor.http.ContentType
import se.gustavkarlsson.chefgpt.api.ApiAttachment

/**
 * The kinds of attachment the agent can actually read. Anything else is rejected on upload,
 * so a user never gets a file into a chat that the agent has to silently ignore.
 */
enum class AttachmentKind {
    Image,
    Pdf,
    Text,
}

fun attachmentKindOrNull(mimeType: String): AttachmentKind? =
    when {
        mimeType.startsWith("image/") -> AttachmentKind.Image
        mimeType.substringBefore(';').trim() == "application/pdf" -> AttachmentKind.Pdf
        mimeType.startsWith("text/") -> AttachmentKind.Text
        else -> null
    }

fun attachmentKindOrNull(contentType: ContentType?): AttachmentKind? =
    contentType?.let { attachmentKindOrNull("${it.contentType}/${it.contentSubtype}") }

val ApiAttachment.kind: AttachmentKind? get() = attachmentKindOrNull(mimeType)

/**
 * The file extension the LLM clients use to label an attachment, taken from the file name when
 * there is one and falling back to the MIME subtype.
 */
val ApiAttachment.format: String
    get() =
        fileName
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }
            ?: mimeType.substringAfter('/').substringBefore(';').trim()
