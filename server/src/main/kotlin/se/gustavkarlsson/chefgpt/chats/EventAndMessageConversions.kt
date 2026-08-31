package se.gustavkarlsson.chefgpt.chats

import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiAgentChatNamed
import se.gustavkarlsson.chefgpt.api.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.ApiAgentMessageChunk
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoined
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserMessage
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.chefGptJson
import se.gustavkarlsson.chefgpt.files.AttachmentKind
import se.gustavkarlsson.chefgpt.files.AttachmentTextLoader
import se.gustavkarlsson.chefgpt.files.format
import se.gustavkarlsson.chefgpt.files.kind
import kotlin.time.Clock
import kotlin.time.Instant
import ai.koog.prompt.message.Message as KoogMessage

fun Event.toApiOrNull(): ApiEvent? =
    when (this) {
        is Event.UserJoined -> {
            ApiUserJoined(
                id = id,
                timestamp = timestamp,
                joinId = joinId,
            )
        }

        is Event.Message -> {
            message.toApiOrNull(id, timestamp, attachments)
        }

        is Event.ChatNamed -> {
            ApiAgentChatNamed(
                id = id,
                timestamp = timestamp,
                name = name,
            )
        }
    }

private fun KoogMessage.toApiOrNull(
    id: EventId,
    timestamp: Instant,
    attachments: List<ApiAttachment>,
): ApiEvent? =
    when (this) {
        is KoogMessage.User -> {
            val text = textContent().takeIf { it.isNotBlank() }
            // Tool-result-only user messages carry no displayable content
            if (text == null && attachments.isEmpty()) {
                null
            } else {
                ApiUserMessage(
                    id = id,
                    timestamp = timestamp,
                    text = text,
                    attachments = attachments,
                )
            }
        }

        is KoogMessage.Assistant -> {
            val chunks =
                textContent()
                    .takeIf { it.isNotBlank() }
                    ?.let { parseAgentMessageChunks(it) }
                    ?.takeIf { it.isNotEmpty() }
                    ?: return null
            ApiAgentMessage(
                id = id,
                timestamp = timestamp,
                chunks = chunks,
            )
        }

        is KoogMessage.System -> {
            null
        }
    }

suspend fun ApiAction.createEvent(textLoader: AttachmentTextLoader): Event =
    when (this) {
        is ApiUserJoinedChat -> {
            Event.UserJoined(EventId.random(), Clock.System.now(), joinId)
        }

        is ApiUserSendsMessage -> {
            val parts =
                buildList {
                    text?.let { add(MessagePart.Text(it)) }
                    attachments.forEach { attachment ->
                        attachment.toMessagePartOrNull(textLoader)?.let(::add)
                    }
                }
            val koogMessage = Message.User(parts, RequestMetaInfo(Clock.System.now()))
            Event.Message(EventId.random(), koogMessage, attachments)
        }
    }

private suspend fun ApiAttachment.toMessagePartOrNull(textLoader: AttachmentTextLoader): MessagePart.Attachment? {
    val source =
        when (kind) {
            AttachmentKind.Image -> {
                AttachmentSource.Image(AttachmentContent.URL(url), format, mimeType, fileName)
            }

            AttachmentKind.Pdf -> {
                AttachmentSource.File(AttachmentContent.URL(url), format, mimeType, fileName)
            }

            // Anthropic only accepts a url as the source of a pdf, so text has to be inlined.
            AttachmentKind.Text -> {
                val text = textLoader.loadText(url) ?: return null
                AttachmentSource.File(AttachmentContent.PlainText(text), format, mimeType, fileName)
            }

            null -> {
                return null
            }
        }
    return MessagePart.Attachment(source)
}

private const val QUESTION_FENCE = "```multiple-choice-question"
private const val CLOSING_FENCE = "```"

// Forgiving: this parses whatever the agent wrote, which we don't control.
private val chunkJson = chefGptJson(strict = false)

@Serializable
private data class MultipleChoiceQuestionJson(
    val question: String,
    val answers: List<String>,
)

fun parseAgentMessageChunks(text: String): List<ApiAgentMessageChunk> {
    val chunks = mutableListOf<ApiAgentMessageChunk>()
    val markdown = StringBuilder()

    fun flushMarkdown() {
        val trimmed = markdown.toString().trim()
        if (trimmed.isNotEmpty()) chunks += ApiAgentMessageChunk.Text(trimmed)
        markdown.clear()
    }

    val lines = text.lines()
    var index = 0
    while (index < lines.size) {
        val multipleChoiceQuestion =
            if (lines[index].trim() == QUESTION_FENCE) {
                val closingIndex =
                    (index + 1 until lines.size)
                        .firstOrNull { lines[it].trim() == CLOSING_FENCE }
                closingIndex?.let { closing ->
                    parseMultipleChoiceQuestionOrNull(lines.subList(index + 1, closing).joinToString("\n"))
                        ?.also { index = closing }
                }
            } else {
                null
            }
        if (multipleChoiceQuestion != null) {
            flushMarkdown()
            chunks += multipleChoiceQuestion
        } else {
            markdown.appendLine(lines[index])
        }
        index++
    }
    flushMarkdown()
    return chunks
}

private fun parseMultipleChoiceQuestionOrNull(content: String): ApiAgentMessageChunk.MultipleChoiceQuestion? {
    val parsed =
        runCatching { chunkJson.decodeFromString<MultipleChoiceQuestionJson>(content) }.getOrNull() ?: return null
    val question = parsed.question.trim()
    val answers = parsed.answers.map { it.trim() }
    if (question.isEmpty() || answers.size < 2 || answers.any { it.isEmpty() }) return null
    return ApiAgentMessageChunk.MultipleChoiceQuestion(question, answers)
}
