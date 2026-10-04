package se.gustavkarlsson.chefgpt.recipes

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.api.common.ImageUrl
import se.gustavkarlsson.chefgpt.files.FileUploader
import kotlin.coroutines.cancellation.CancellationException

/**
 * Downloads an image from the recipe's own site and uploads it to our trusted host, so the saved
 * url passes the photo allowlist. Returns null when the image cannot be downloaded or uploaded.
 */
class RecipeImageRehoster(
    private val fileUploader: FileUploader,
) : AutoCloseable {
    private val client =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
            }
        }

    suspend fun rehost(imageUrl: ImageUrl): ImageUrl? {
        if (recipePhotoUrlOrNull(imageUrl.value) != null) return imageUrl
        return try {
            val response = client.get(imageUrl.value)
            val contentType = response.contentType()
            if (contentType?.contentType != "image") return null
            val bytes = response.bodyAsChannel().readRemaining(MAX_IMAGE_BYTES + 1).readByteArray()
            if (bytes.size > MAX_IMAGE_BYTES) return null
            val uploaded =
                fileUploader.uploadFile(
                    readChannel = ByteReadChannel(bytes),
                    contentType = contentType,
                    fileName = fileNameOrNull(imageUrl.value),
                ) ?: return null
            ImageUrl(uploaded.url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Failed to rehost recipe image: ${e.message}")
            null
        }
    }

    override fun close() {
        client.close()
    }
}

private const val MAX_IMAGE_BYTES = 10L * 1024 * 1024

private fun fileNameOrNull(url: String): String? =
    url.substringAfterLast('/').substringBefore('?').takeIf { it.contains('.') }

private val logger = LoggerFactory.getLogger("RecipeImageRehoster")
