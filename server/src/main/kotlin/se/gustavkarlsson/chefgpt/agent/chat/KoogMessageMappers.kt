package se.gustavkarlsson.chefgpt.agent.chat

import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import se.gustavkarlsson.chefgpt.chats.AttachmentPart
import se.gustavkarlsson.chefgpt.chats.ChatMessage
import se.gustavkarlsson.chefgpt.chats.ReasoningPart
import se.gustavkarlsson.chefgpt.chats.TextPart
import se.gustavkarlsson.chefgpt.chats.ToolCallPart
import se.gustavkarlsson.chefgpt.chats.ToolResultPart
import se.gustavkarlsson.chefgpt.files.FileKind
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.files.format
import se.gustavkarlsson.chefgpt.files.kind
import ai.koog.prompt.message.Message as KoogMessage
import se.gustavkarlsson.chefgpt.chats.AttachmentContent as DomainAttachmentContent
import se.gustavkarlsson.chefgpt.chats.AttachmentSource as DomainAttachmentSource

// All conversions between Koog and domain models.
//
// cacheControl values are dropped when replaying a message to the LLM: they are provider-specific
// types our Json never serializes non-null, so stored messages never carry one.

fun KoogMessage.toDomain(json: Json): ChatMessage =
    when (this) {
        is KoogMessage.User -> {
            ChatMessage.User(
                id = id,
                timestamp = metaInfo.timestamp,
                metadata = metaInfo.metadata?.toString(),
                parts = parts.map { it.toDomain(json) },
            )
        }

        is KoogMessage.Assistant -> {
            ChatMessage.Assistant(
                id = id,
                timestamp = metaInfo.timestamp,
                metadata = metaInfo.metadata?.toString(),
                parts = parts.map { it.toDomain(json) },
                finishReason = finishReason,
                rawResponse = rawResponse?.toString(),
                totalTokensCount = metaInfo.totalTokensCount,
                inputTokensCount = metaInfo.inputTokensCount,
                outputTokensCount = metaInfo.outputTokensCount,
                modelId = metaInfo.modelId,
            )
        }

        is KoogMessage.System -> {
            ChatMessage.System(
                id = id,
                timestamp = metaInfo.timestamp,
                metadata = metaInfo.metadata?.toString(),
                parts = parts.map { TextPart(it.text, it.cacheControl?.toString()) },
            )
        }
    }

fun ChatMessage.toKoog(json: Json): KoogMessage =
    when (this) {
        is ChatMessage.User -> {
            KoogMessage.User(
                parts = parts.map { it.toKoog(json) },
                metaInfo = RequestMetaInfo(timestamp, metadata.toJsonObjectOrNull(json)),
                id = id,
            )
        }

        is ChatMessage.Assistant -> {
            KoogMessage.Assistant(
                parts = parts.map { it.toKoog(json) },
                metaInfo =
                    ResponseMetaInfo(
                        timestamp = timestamp,
                        totalTokensCount = totalTokensCount,
                        inputTokensCount = inputTokensCount,
                        outputTokensCount = outputTokensCount,
                        modelId = modelId,
                        metadata = metadata.toJsonObjectOrNull(json),
                    ),
                finishReason = finishReason,
                rawResponse = rawResponse.toJsonObjectOrNull(json),
                id = id,
            )
        }

        is ChatMessage.System -> {
            KoogMessage.System(
                parts = parts.map { MessagePart.Text(it.text) },
                metaInfo = RequestMetaInfo(timestamp, metadata.toJsonObjectOrNull(json)),
                id = id,
            )
        }
    }

fun UploadedFile.toImageAttachmentOrNull(): AttachmentSource.Image? {
    if (kind != FileKind.Image) return null
    return AttachmentSource.Image(
        content = AttachmentContent.URL(url),
        format = format,
        mimeType = mimeType,
        fileName = fileName,
    )
}

private fun MessagePart.RequestPart.toDomain(json: Json): se.gustavkarlsson.chefgpt.chats.RequestPart =
    when (this) {
        is MessagePart.Text -> {
            TextPart(text, cacheControl?.toString())
        }

        is MessagePart.Attachment -> {
            AttachmentPart(source.toDomain(json), cacheControl?.toString())
        }

        is MessagePart.Tool.Result -> {
            ToolResultPart(
                id = id,
                tool = tool,
                parts = parts.map { it.toDomain(json) },
                isError = isError,
                cacheControl = cacheControl?.toString(),
            )
        }
    }

private fun MessagePart.ResponsePart.toDomain(json: Json): se.gustavkarlsson.chefgpt.chats.ResponsePart =
    when (this) {
        is MessagePart.Text -> {
            TextPart(text, cacheControl?.toString())
        }

        is MessagePart.Attachment -> {
            AttachmentPart(source.toDomain(json), cacheControl?.toString())
        }

        is MessagePart.Reasoning -> {
            ReasoningPart(
                content = content,
                summary = summary,
                encrypted = encrypted,
                id = id,
                cacheControl = cacheControl?.toString(),
            )
        }

        is MessagePart.Tool.Call -> {
            ToolCallPart(
                id = id,
                tool = tool,
                args = args,
                cacheControl = cacheControl?.toString(),
            )
        }
    }

private fun MessagePart.ContentPart.toDomain(json: Json): se.gustavkarlsson.chefgpt.chats.ContentPart =
    when (this) {
        is MessagePart.Text -> TextPart(text, cacheControl?.toString())
        is MessagePart.Attachment -> AttachmentPart(source.toDomain(json), cacheControl?.toString())
    }

private fun se.gustavkarlsson.chefgpt.chats.RequestPart.toKoog(json: Json): MessagePart.RequestPart =
    when (this) {
        is TextPart -> {
            MessagePart.Text(text)
        }

        is AttachmentPart -> {
            MessagePart.Attachment(source.toKoog(json))
        }

        is ToolResultPart -> {
            MessagePart.Tool.Result(
                id = id,
                tool = tool,
                parts = parts.map { it.toKoog(json) },
                isError = isError,
            )
        }
    }

private fun se.gustavkarlsson.chefgpt.chats.ResponsePart.toKoog(json: Json): MessagePart.ResponsePart =
    when (this) {
        is TextPart -> {
            MessagePart.Text(text)
        }

        is AttachmentPart -> {
            MessagePart.Attachment(source.toKoog(json))
        }

        is ReasoningPart -> {
            MessagePart.Reasoning(
                content = content,
                summary = summary,
                encrypted = encrypted,
                id = id,
            )
        }

        is ToolCallPart -> {
            MessagePart.Tool.Call(
                id = id,
                tool = tool,
                args = args,
            )
        }
    }

private fun se.gustavkarlsson.chefgpt.chats.ContentPart.toKoog(json: Json): MessagePart.ContentPart =
    when (this) {
        is TextPart -> MessagePart.Text(text)
        is AttachmentPart -> MessagePart.Attachment(source.toKoog(json))
    }

private fun AttachmentSource.toDomain(json: Json): DomainAttachmentSource =
    when (this) {
        is AttachmentSource.Image -> DomainAttachmentSource.Image(content.toDomain(json), format, mimeType, fileName)
        is AttachmentSource.Video -> DomainAttachmentSource.Video(content.toDomain(json), format, mimeType, fileName)
        is AttachmentSource.Audio -> DomainAttachmentSource.Audio(content.toDomain(json), format, mimeType, fileName)
        is AttachmentSource.File -> DomainAttachmentSource.File(content.toDomain(json), format, mimeType, fileName)
    }

private fun DomainAttachmentSource.toKoog(json: Json): AttachmentSource =
    when (this) {
        is DomainAttachmentSource.Image -> AttachmentSource.Image(content.toKoog(json), format, mimeType, fileName)
        is DomainAttachmentSource.Video -> AttachmentSource.Video(content.toKoog(json), format, mimeType, fileName)
        is DomainAttachmentSource.Audio -> AttachmentSource.Audio(content.toKoog(json), format, mimeType, fileName)
        is DomainAttachmentSource.File -> AttachmentSource.File(content.toKoog(json), format, mimeType, fileName)
    }

private fun AttachmentContent.toDomain(json: Json): DomainAttachmentContent =
    when (this) {
        is AttachmentContent.PlainText -> DomainAttachmentContent.PlainText(text)
        is AttachmentContent.URL -> DomainAttachmentContent.Url(url)
        is AttachmentContent.Binary.Bytes -> DomainAttachmentContent.BinaryBytes(data)
        is AttachmentContent.Binary.Base64 -> DomainAttachmentContent.BinaryBase64(base64)
    }

private fun DomainAttachmentContent.toKoog(json: Json): AttachmentContent =
    when (this) {
        is DomainAttachmentContent.PlainText -> AttachmentContent.PlainText(text)
        is DomainAttachmentContent.Url -> AttachmentContent.URL(url)
        is DomainAttachmentContent.BinaryBytes -> AttachmentContent.Binary.Bytes(data)
        is DomainAttachmentContent.BinaryBase64 -> AttachmentContent.Binary.Base64(base64)
    }

private fun String?.toJsonObjectOrNull(json: Json) = this?.let { json.parseToJsonElement(it).jsonObject }
