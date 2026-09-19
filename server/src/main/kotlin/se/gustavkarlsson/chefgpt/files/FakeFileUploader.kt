package se.gustavkarlsson.chefgpt.files

import io.ktor.http.ContentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import se.gustavkarlsson.chefgpt.api.ApiUploadedFile

class FakeFileUploader : FileUploader {
    override suspend fun uploadFile(
        readChannel: ByteReadChannel,
        contentType: ContentType?,
        fileName: String?,
    ): ApiUploadedFile {
        readChannel.readRemaining().close()
        val mimeType = contentType?.let { "${it.contentType}/${it.contentSubtype}" } ?: "image/jpeg"
        val url =
            when (fileKindOrNull(mimeType)) {
                FileKind.Image, null -> "https://cataas.com/cat"
                FileKind.Pdf -> "https://example.com/fake.pdf"
                FileKind.Text -> "https://example.com/fake.txt"
            }
        return ApiUploadedFile(url = url, mimeType = mimeType, fileName = fileName)
    }
}
