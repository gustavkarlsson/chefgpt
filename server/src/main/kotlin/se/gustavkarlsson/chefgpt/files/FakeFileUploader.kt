package se.gustavkarlsson.chefgpt.files

import io.ktor.http.ContentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining

class FakeFileUploader : FileUploader {
    override suspend fun uploadFile(
        readChannel: ByteReadChannel,
        contentType: ContentType?,
        fileName: String?,
    ): UploadedFile {
        readChannel.readRemaining().close()
        val mimeType = contentType?.let { "${it.contentType}/${it.contentSubtype}" } ?: "image/jpeg"
        val url =
            when (fileKindOrNull(mimeType)) {
                FileKind.Image, null -> "https://cataas.com/cat"
                FileKind.Pdf -> "https://example.com/fake.pdf"
                FileKind.Text -> "https://example.com/fake.txt"
            }
        return UploadedFile(url = url, mimeType = mimeType, fileName = fileName)
    }
}
