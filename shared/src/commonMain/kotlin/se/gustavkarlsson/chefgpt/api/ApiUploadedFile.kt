package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Carries the original file name on upload, which multipart would otherwise be needed for.
const val FILE_NAME_HEADER = "X-File-Name"

/**
 * A file the user has uploaded. Reachable at [url].
 */
@Serializable
@SerialName("api-uploaded-file")
data class ApiUploadedFile(
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

    val isImage: Boolean get() = mimeType.startsWith("image/")
}
