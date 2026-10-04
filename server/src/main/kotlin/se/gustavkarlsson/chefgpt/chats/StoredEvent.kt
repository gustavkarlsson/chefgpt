package se.gustavkarlsson.chefgpt.chats

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import se.gustavkarlsson.chefgpt.api.common.EventId
import se.gustavkarlsson.chefgpt.api.common.JoinId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant

// Database models: the serialized form of events as stored in the event.json column. These
// classes define the whole storage format; changing their fields or serial names is a migration,
// not a refactor.

@Serializable
sealed interface StoredEvent

@Serializable
@SerialName("message")
data class StoredMessageEvent(
    val id: EventId,
    val message: StoredMessage,
    val attachments: List<StoredUploadedFile>,
) : StoredEvent

@Serializable
@SerialName("user-joined")
data class StoredUserJoined(
    val id: EventId,
    val timestamp: Instant,
    val joinId: JoinId,
) : StoredEvent

@Serializable
@SerialName("chat-named")
data class StoredChatNamed(
    val id: EventId,
    val timestamp: Instant,
    val name: String,
) : StoredEvent

@Serializable
data class StoredUploadedFile(
    val url: String,
    val mimeType: String,
    val fileName: String?,
)

@Serializable
sealed interface StoredMessage

@Serializable
@SerialName("user-message")
data class StoredUserMessage(
    val parts: List<StoredRequestPart>,
    val metaInfo: StoredRequestMetaInfo,
    val id: String?,
) : StoredMessage

@Serializable
@SerialName("assistant-message")
data class StoredAssistantMessage(
    val parts: List<StoredResponsePart>,
    val metaInfo: StoredResponseMetaInfo,
    val finishReason: String?,
    val rawResponse: JsonObject?,
    val id: String?,
) : StoredMessage

@Serializable
@SerialName("system-message")
data class StoredSystemMessage(
    val parts: List<StoredTextPart>,
    val metaInfo: StoredRequestMetaInfo,
    val id: String?,
) : StoredMessage

@Serializable
data class StoredRequestMetaInfo(
    val timestamp: Instant,
    val metadata: JsonObject?,
)

@Serializable
data class StoredResponseMetaInfo(
    val timestamp: Instant,
    val totalTokensCount: Int?,
    val inputTokensCount: Int?,
    val outputTokensCount: Int?,
    val modelId: String?,
    val metadata: JsonObject?,
)

@Serializable
sealed interface StoredRequestPart

@Serializable
sealed interface StoredResponsePart

@Serializable
sealed interface StoredContentPart :
    StoredRequestPart,
    StoredResponsePart

@Serializable
@SerialName("text")
data class StoredTextPart(
    val text: String,
    val cacheControl: JsonObject?,
) : StoredContentPart

@Serializable
@SerialName("attachment")
data class StoredAttachmentPart(
    val source: StoredAttachmentSource,
    val cacheControl: JsonObject?,
) : StoredContentPart

@Serializable
@SerialName("reasoning")
data class StoredReasoningPart(
    val content: List<String>,
    val summary: List<String>?,
    val encrypted: String?,
    val id: String?,
    val cacheControl: JsonObject?,
) : StoredResponsePart

@Serializable
@SerialName("tool-call")
data class StoredToolCallPart(
    val id: String?,
    val tool: String,
    val args: String,
    val cacheControl: JsonObject?,
) : StoredResponsePart

@Serializable
@SerialName("tool-result")
data class StoredToolResultPart(
    val id: String?,
    val tool: String,
    val parts: List<StoredContentPart>,
    val isError: Boolean,
    val cacheControl: JsonObject?,
) : StoredRequestPart

@Serializable
sealed interface StoredAttachmentSource

@Serializable
@SerialName("image")
data class StoredImageSource(
    val content: StoredAttachmentContent,
    val format: String,
    val mimeType: String,
    val fileName: String?,
) : StoredAttachmentSource

@Serializable
@SerialName("video")
data class StoredVideoSource(
    val content: StoredAttachmentContent,
    val format: String,
    val mimeType: String,
    val fileName: String?,
) : StoredAttachmentSource

@Serializable
@SerialName("audio")
data class StoredAudioSource(
    val content: StoredAttachmentContent,
    val format: String,
    val mimeType: String,
    val fileName: String?,
) : StoredAttachmentSource

@Serializable
@SerialName("file")
data class StoredFileSource(
    val content: StoredAttachmentContent,
    val format: String,
    val mimeType: String,
    val fileName: String?,
) : StoredAttachmentSource

@Serializable
sealed interface StoredAttachmentContent

@Serializable
@SerialName("plain-text")
data class StoredPlainTextContent(
    val text: String,
) : StoredAttachmentContent

@Serializable
@SerialName("url")
data class StoredUrlContent(
    val url: String,
) : StoredAttachmentContent

@Serializable
@SerialName("bytes")
data class StoredBinaryBytesContent(
    @SerialName("base64")
    @Serializable(with = Base64ByteArraySerializer::class)
    val data: ByteArray,
) : StoredAttachmentContent

@Serializable
@SerialName("base64")
data class StoredBinaryBase64Content(
    val base64: String,
) : StoredAttachmentContent

@OptIn(ExperimentalEncodingApi::class)
object Base64ByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Base64ByteArray", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ByteArray,
    ) {
        encoder.encodeString(Base64.encode(value))
    }

    override fun deserialize(decoder: Decoder): ByteArray = Base64.decode(decoder.decodeString())
}

// Event <-> StoredEvent

fun Event.toStored(json: Json): StoredEvent =
    when (this) {
        is Event.UserJoined -> {
            StoredUserJoined(id, timestamp, joinId)
        }

        is Event.Message -> {
            StoredMessageEvent(
                id = id,
                message = message.toStored(json),
                attachments = attachments.map { StoredUploadedFile(it.url, it.mimeType, it.fileName) },
            )
        }

        is Event.ChatNamed -> {
            StoredChatNamed(id, timestamp, name)
        }
    }

fun StoredEvent.toDomain(json: Json): Event =
    when (this) {
        is StoredUserJoined -> {
            Event.UserJoined(id, timestamp, joinId)
        }

        is StoredMessageEvent -> {
            Event.Message(
                id = id,
                message = message.toDomain(json),
                attachments = attachments.map { UploadedFile(it.url, it.mimeType, it.fileName) },
            )
        }

        is StoredChatNamed -> {
            Event.ChatNamed(id, timestamp, name)
        }
    }

private fun ChatMessage.toStored(json: Json): StoredMessage =
    when (this) {
        is ChatMessage.User -> {
            StoredUserMessage(
                parts = parts.map { it.toStored(json) },
                metaInfo = StoredRequestMetaInfo(timestamp, metadata.toJsonObjectOrNull(json)),
                id = id,
            )
        }

        is ChatMessage.Assistant -> {
            StoredAssistantMessage(
                parts = parts.map { it.toStored(json) },
                metaInfo =
                    StoredResponseMetaInfo(
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
            StoredSystemMessage(
                parts = parts.map { StoredTextPart(it.text, it.cacheControl.toJsonObjectOrNull(json)) },
                metaInfo = StoredRequestMetaInfo(timestamp, metadata.toJsonObjectOrNull(json)),
                id = id,
            )
        }
    }

private fun StoredMessage.toDomain(json: Json): ChatMessage =
    when (this) {
        is StoredUserMessage -> {
            ChatMessage.User(
                id = id,
                timestamp = metaInfo.timestamp,
                metadata = metaInfo.metadata?.toString(),
                parts = parts.map { it.toDomain(json) },
            )
        }

        is StoredAssistantMessage -> {
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

        is StoredSystemMessage -> {
            ChatMessage.System(
                id = id,
                timestamp = metaInfo.timestamp,
                metadata = metaInfo.metadata?.toString(),
                parts = parts.map { TextPart(it.text, it.cacheControl?.toString()) },
            )
        }
    }

private fun RequestPart.toStored(json: Json): StoredRequestPart =
    when (this) {
        is TextPart -> {
            StoredTextPart(text, cacheControl.toJsonObjectOrNull(json))
        }

        is AttachmentPart -> {
            StoredAttachmentPart(source.toStored(), cacheControl.toJsonObjectOrNull(json))
        }

        is ToolResultPart -> {
            StoredToolResultPart(
                id = id,
                tool = tool,
                parts = parts.map { it.toStored(json) },
                isError = isError,
                cacheControl = cacheControl.toJsonObjectOrNull(json),
            )
        }
    }

private fun ResponsePart.toStored(json: Json): StoredResponsePart =
    when (this) {
        is TextPart -> {
            StoredTextPart(text, cacheControl.toJsonObjectOrNull(json))
        }

        is AttachmentPart -> {
            StoredAttachmentPart(source.toStored(), cacheControl.toJsonObjectOrNull(json))
        }

        is ReasoningPart -> {
            StoredReasoningPart(
                content = content,
                summary = summary,
                encrypted = encrypted,
                id = id,
                cacheControl = cacheControl.toJsonObjectOrNull(json),
            )
        }

        is ToolCallPart -> {
            StoredToolCallPart(
                id = id,
                tool = tool,
                args = args,
                cacheControl = cacheControl.toJsonObjectOrNull(json),
            )
        }
    }

private fun ContentPart.toStored(json: Json): StoredContentPart =
    when (this) {
        is TextPart -> StoredTextPart(text, cacheControl.toJsonObjectOrNull(json))
        is AttachmentPart -> StoredAttachmentPart(source.toStored(), cacheControl.toJsonObjectOrNull(json))
    }

private fun StoredRequestPart.toDomain(json: Json): RequestPart =
    when (this) {
        is StoredTextPart -> {
            TextPart(text, cacheControl?.toString())
        }

        is StoredAttachmentPart -> {
            AttachmentPart(source.toDomain(), cacheControl?.toString())
        }

        is StoredToolResultPart -> {
            ToolResultPart(
                id = id,
                tool = tool,
                parts = parts.map { it.toDomain(json) },
                isError = isError,
                cacheControl = cacheControl?.toString(),
            )
        }
    }

private fun StoredResponsePart.toDomain(json: Json): ResponsePart =
    when (this) {
        is StoredTextPart -> {
            TextPart(text, cacheControl?.toString())
        }

        is StoredAttachmentPart -> {
            AttachmentPart(source.toDomain(), cacheControl?.toString())
        }

        is StoredReasoningPart -> {
            ReasoningPart(
                content = content,
                summary = summary,
                encrypted = encrypted,
                id = id,
                cacheControl = cacheControl?.toString(),
            )
        }

        is StoredToolCallPart -> {
            ToolCallPart(
                id = id,
                tool = tool,
                args = args,
                cacheControl = cacheControl?.toString(),
            )
        }
    }

private fun StoredContentPart.toDomain(json: Json): ContentPart =
    when (this) {
        is StoredTextPart -> TextPart(text, cacheControl?.toString())
        is StoredAttachmentPart -> AttachmentPart(source.toDomain(), cacheControl?.toString())
    }

private fun AttachmentSource.toStored(): StoredAttachmentSource =
    when (this) {
        is AttachmentSource.Image -> StoredImageSource(content.toStored(), format, mimeType, fileName)
        is AttachmentSource.Video -> StoredVideoSource(content.toStored(), format, mimeType, fileName)
        is AttachmentSource.Audio -> StoredAudioSource(content.toStored(), format, mimeType, fileName)
        is AttachmentSource.File -> StoredFileSource(content.toStored(), format, mimeType, fileName)
    }

private fun StoredAttachmentSource.toDomain(): AttachmentSource =
    when (this) {
        is StoredImageSource -> AttachmentSource.Image(content.toDomain(), format, mimeType, fileName)
        is StoredVideoSource -> AttachmentSource.Video(content.toDomain(), format, mimeType, fileName)
        is StoredAudioSource -> AttachmentSource.Audio(content.toDomain(), format, mimeType, fileName)
        is StoredFileSource -> AttachmentSource.File(content.toDomain(), format, mimeType, fileName)
    }

private fun AttachmentContent.toStored(): StoredAttachmentContent =
    when (this) {
        is AttachmentContent.PlainText -> StoredPlainTextContent(text)
        is AttachmentContent.Url -> StoredUrlContent(url)
        is AttachmentContent.BinaryBase64 -> StoredBinaryBase64Content(base64)
        is AttachmentContent.BinaryBytes -> StoredBinaryBytesContent(data)
    }

private fun StoredAttachmentContent.toDomain(): AttachmentContent =
    when (this) {
        is StoredPlainTextContent -> AttachmentContent.PlainText(text)
        is StoredUrlContent -> AttachmentContent.Url(url)
        is StoredBinaryBase64Content -> AttachmentContent.BinaryBase64(base64)
        is StoredBinaryBytesContent -> AttachmentContent.BinaryBytes(data)
    }

private fun String?.toJsonObjectOrNull(json: Json): JsonObject? = this?.let { json.parseToJsonElement(it).jsonObject }
