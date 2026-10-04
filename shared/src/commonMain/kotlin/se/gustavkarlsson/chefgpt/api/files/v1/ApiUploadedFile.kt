package se.gustavkarlsson.chefgpt.api.files.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
