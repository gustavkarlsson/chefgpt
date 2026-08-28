package se.gustavkarlsson.chefgpt.files

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readString

/**
 * Anthropic only accepts a url as the source of a *pdf* document, so text attachments have to be
 * inlined. Reading them here means it happens once per message rather than on every prompt build.
 */
class HttpAttachmentTextLoader :
    AttachmentTextLoader,
    AutoCloseable {
    private val client =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
            }
        }

    override suspend fun loadText(url: String): String? =
        try {
            val bytes = client.get(url).bodyAsChannel().readRemaining(MAX_BYTES)
            bytes.readString().takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            // TODO log error
            null
        }

    override fun close() {
        client.close()
    }
}

// Enough for any recipe, small enough that one file can't crowd out the conversation.
private const val MAX_BYTES = 200L * 1024
