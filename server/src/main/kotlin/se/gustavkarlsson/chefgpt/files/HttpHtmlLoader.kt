package se.gustavkarlsson.chefgpt.files

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readString
import kotlin.coroutines.cancellation.CancellationException

/**
 * Loads the html of a page over HTTP. Text attachments have to be inlined into the prompt, and
 * reading them here means it happens once per message rather than on every prompt build.
 */
class HttpHtmlLoader :
    HtmlLoader,
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
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // TODO log error
            null
        }

    override fun close() {
        client.close()
    }
}

// Enough for any recipe page, small enough that one page can't crowd out the conversation.
private const val MAX_BYTES = 200L * 1024
