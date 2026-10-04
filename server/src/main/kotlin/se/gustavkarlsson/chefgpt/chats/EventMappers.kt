package se.gustavkarlsson.chefgpt.chats

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiAgentChatNamed
import se.gustavkarlsson.chefgpt.api.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.ApiAgentMessageChunk
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUploadedFile
import se.gustavkarlsson.chefgpt.api.ApiUserJoined
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserMessage
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.chefGptJson
import se.gustavkarlsson.chefgpt.files.FileKind
import se.gustavkarlsson.chefgpt.files.HtmlLoader
import se.gustavkarlsson.chefgpt.files.format
import se.gustavkarlsson.chefgpt.files.kind
import se.gustavkarlsson.chefgpt.files.toApi
import se.gustavkarlsson.chefgpt.files.toDomain
import kotlin.time.Clock
import kotlin.time.Instant

// Api -> Domain

suspend fun ApiAction.toEvent(htmlLoader: HtmlLoader): Event =
    when (this) {
        is ApiUserJoinedChat -> {
            Event.UserJoined(EventId.random(), Clock.System.now(), joinId)
        }

        is ApiUserSendsMessage -> {
            val parts =
                buildList {
                    text?.let { add(TextPart(it, cacheControl = null)) }
                    attachments.forEach { attachment ->
                        attachment.toPartOrNull(htmlLoader)?.let(::add)
                    }
                }
            val message =
                ChatMessage.User(
                    id = null,
                    timestamp = Clock.System.now(),
                    metadata = null,
                    parts = parts,
                )
            Event.Message(EventId.random(), message, attachments.map { it.toDomain() })
        }
    }

private suspend fun ApiUploadedFile.toPartOrNull(htmlLoader: HtmlLoader): RequestPart? {
    val file = toDomain()
    val source =
        when (file.kind) {
            FileKind.Image -> {
                AttachmentSource.Image(AttachmentContent.Url(file.url), file.format, file.mimeType, file.fileName)
            }

            FileKind.Pdf -> {
                AttachmentSource.File(AttachmentContent.Url(file.url), file.format, file.mimeType, file.fileName)
            }

            // Anthropic only accepts a url as the source of a pdf, so text has to be inlined.
            FileKind.Text -> {
                val text = htmlLoader.loadText(file.url) ?: return null
                AttachmentSource.File(AttachmentContent.PlainText(text), file.format, file.mimeType, file.fileName)
            }

            null -> {
                return null
            }
        }
    return AttachmentPart(source, cacheControl = null)
}

// Domain -> Api

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
            message.toApiOrNull(id, timestamp, attachments.map { it.toApi() })
        }

        is Event.ChatNamed -> {
            ApiAgentChatNamed(
                id = id,
                timestamp = timestamp,
                name = name,
            )
        }
    }

private fun ChatMessage.toApiOrNull(
    id: EventId,
    timestamp: Instant,
    attachments: List<ApiUploadedFile>,
): ApiEvent? =
    when (this) {
        is ChatMessage.User -> {
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

        is ChatMessage.Assistant -> {
            val chunks =
                textContent()
                    .takeIf { it.isNotBlank() }
                    ?.let(::parseAgentMessageChunks)
                    ?.takeIf { it.isNotEmpty() }
                    ?: return null
            ApiAgentMessage(
                id = id,
                timestamp = timestamp,
                chunks = chunks,
            )
        }

        is ChatMessage.System -> {
            null
        }
    }

// Parses the assistant's text into displayable chunks, including any multiple-choice question.
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

private const val QUESTION_FENCE = "```multiple-choice-question"
private const val CLOSING_FENCE = "```"

// Forgiving: this parses whatever the agent wrote, which we don't control.
private val chunkJson = chefGptJson(strict = false)

@Serializable
private data class MultipleChoiceQuestionJson(
    val question: String,
    val answers: List<String>,
)
