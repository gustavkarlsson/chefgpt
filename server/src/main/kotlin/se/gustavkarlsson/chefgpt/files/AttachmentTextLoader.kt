package se.gustavkarlsson.chefgpt.files

interface AttachmentTextLoader {
    // Returns null if the text could not be read.
    suspend fun loadText(url: String): String?
}
