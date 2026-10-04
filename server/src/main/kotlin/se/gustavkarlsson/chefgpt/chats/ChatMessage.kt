package se.gustavkarlsson.chefgpt.chats

import kotlin.time.Instant

/**
 * A single turn in a chat transcript, stored inside [Event.Message]. Mirrors the message an agent
 * sends to and receives from the LLM — including tool calls and results — so the transcript can be
 * replayed into the prompt each turn. Free-form JSON fields are kept as raw strings.
 */
sealed interface ChatMessage {
    val id: String?
    val timestamp: Instant

    // The free-form JSON object the message carried, or null.
    val metadata: String?

    data class User(
        override val id: String?,
        override val timestamp: Instant,
        override val metadata: String?,
        val parts: List<RequestPart>,
    ) : ChatMessage

    data class Assistant(
        override val id: String?,
        override val timestamp: Instant,
        override val metadata: String?,
        val parts: List<ResponsePart>,
        val finishReason: String?,
        // The provider's raw JSON response body, or null.
        val rawResponse: String?,
        val totalTokensCount: Int?,
        val inputTokensCount: Int?,
        val outputTokensCount: Int?,
        val modelId: String?,
    ) : ChatMessage

    data class System(
        override val id: String?,
        override val timestamp: Instant,
        override val metadata: String?,
        val parts: List<TextPart>,
    ) : ChatMessage
}

fun ChatMessage.textContent(): String =
    when (this) {
        is ChatMessage.User -> parts.filterIsInstance<TextPart>()
        is ChatMessage.Assistant -> parts.filterIsInstance<TextPart>()
        is ChatMessage.System -> parts
    }.joinToString("\n") { it.text }

/** A part sent to the LLM. */
sealed interface RequestPart

/** A part received from the LLM. */
sealed interface ResponsePart

/** A part valid in both directions. */
sealed interface ContentPart :
    RequestPart,
    ResponsePart

data class TextPart(
    val text: String,
    // The free-form JSON object of the cache-control directive, or null.
    val cacheControl: String?,
) : ContentPart

data class AttachmentPart(
    val source: AttachmentSource,
    val cacheControl: String?,
) : ContentPart

data class ReasoningPart(
    val content: List<String>,
    val summary: List<String>?,
    val encrypted: String?,
    val id: String?,
    val cacheControl: String?,
) : ResponsePart

data class ToolCallPart(
    val id: String?,
    val tool: String,
    val args: String,
    val cacheControl: String?,
) : ResponsePart

data class ToolResultPart(
    val id: String?,
    val tool: String,
    val parts: List<ContentPart>,
    val isError: Boolean,
    val cacheControl: String?,
) : RequestPart

sealed interface AttachmentSource {
    val content: AttachmentContent
    val format: String
    val mimeType: String
    val fileName: String?

    data class Image(
        override val content: AttachmentContent,
        override val format: String,
        override val mimeType: String,
        override val fileName: String?,
    ) : AttachmentSource

    data class Video(
        override val content: AttachmentContent,
        override val format: String,
        override val mimeType: String,
        override val fileName: String?,
    ) : AttachmentSource

    data class Audio(
        override val content: AttachmentContent,
        override val format: String,
        override val mimeType: String,
        override val fileName: String?,
    ) : AttachmentSource

    data class File(
        override val content: AttachmentContent,
        override val format: String,
        override val mimeType: String,
        override val fileName: String?,
    ) : AttachmentSource
}

sealed interface AttachmentContent {
    data class PlainText(
        val text: String,
    ) : AttachmentContent

    data class Url(
        val url: String,
    ) : AttachmentContent

    data class BinaryBase64(
        val base64: String,
    ) : AttachmentContent

    data class BinaryBytes(
        val data: ByteArray,
    ) : AttachmentContent
}
