package se.gustavkarlsson.chefgpt.images

import io.ktor.http.ContentType
import io.ktor.utils.io.ByteReadChannel
import se.gustavkarlsson.chefgpt.api.ImageUrl

interface ImageUploader {
    // TODO Return Result
    suspend fun uploadImage(
        readChannel: ByteReadChannel,
        contentType: ContentType? = null,
    ): ImageUrl
}
