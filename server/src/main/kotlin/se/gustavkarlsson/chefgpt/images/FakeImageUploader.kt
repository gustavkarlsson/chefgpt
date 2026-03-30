package se.gustavkarlsson.chefgpt.images

import io.ktor.http.ContentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import se.gustavkarlsson.chefgpt.api.ImageUrl

class FakeImageUploader : ImageUploader {
    override suspend fun uploadImage(
        readChannel: ByteReadChannel,
        contentType: ContentType?,
    ): ImageUrl {
        readChannel.readRemaining().close()
        return ImageUrl("https://cataas.com/cat")
    }
}
