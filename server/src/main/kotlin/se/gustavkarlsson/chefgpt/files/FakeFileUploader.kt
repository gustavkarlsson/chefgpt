package se.gustavkarlsson.chefgpt.files

import io.ktor.http.ContentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import se.gustavkarlsson.chefgpt.api.ApiAttachment

class FakeFileUploader : FileUploader {
    override suspend fun uploadFile(
        readChannel: ByteReadChannel,
        contentType: ContentType?,
        fileName: String?,
    ): ApiAttachment {
        readChannel.readRemaining().close()
        val mimeType = contentType?.let { "${it.contentType}/${it.contentSubtype}" } ?: "image/jpeg"
        val url =
            when (attachmentKindOrNull(mimeType)) {
                AttachmentKind.Image, null -> "https://cataas.com/cat"
                AttachmentKind.Pdf -> "https://example.com/fake.pdf"
                AttachmentKind.Text -> "https://example.com/fake.txt"
            }
        return ApiAttachment(url = url, mimeType = mimeType, fileName = fileName)
    }
}
