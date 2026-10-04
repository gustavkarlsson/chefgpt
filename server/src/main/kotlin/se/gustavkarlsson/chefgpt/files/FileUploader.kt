package se.gustavkarlsson.chefgpt.files

import io.ktor.http.ContentType
import io.ktor.utils.io.ByteReadChannel

interface FileUploader {
    // Returns null if the upload failed.
    suspend fun uploadFile(
        readChannel: ByteReadChannel,
        contentType: ContentType? = null,
        fileName: String? = null,
    ): UploadedFile?
}
