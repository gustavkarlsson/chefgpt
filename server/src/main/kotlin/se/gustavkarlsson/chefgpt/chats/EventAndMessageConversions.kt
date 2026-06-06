package se.gustavkarlsson.chefgpt.chats

import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoined
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserMessage
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.api.ImageUrl
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
            message.toApiOrNull(id, timestamp)
        }
    }

private fun KoogMessage.toApiOrNull(
    id: EventId,
    timestamp: Instant,
): ApiEvent? =
    when (this) {
        is KoogMessage.User -> {
            val text = textContent().takeIf { it.isNotBlank() }
            val imageUrl = imageUrlOrNull()
            // Tool-result-only user messages carry no displayable content
            if (text == null && imageUrl == null) {
                null
            } else {
                ApiUserMessage(
                    id = id,
                    timestamp = timestamp,
                    text = text,
                    imageUrl = imageUrl,
                )
            }
        }

        is KoogMessage.Assistant -> {
            textContent()
                .takeIf { it.isNotBlank() }
                ?.let { text ->
                    ApiAgentMessage(
                        id = id,
                        timestamp = timestamp,
                        text = text,
                    )
                }
        }

        is KoogMessage.System -> {
            null
        }
    }

private fun KoogMessage.User.imageUrlOrNull(): ImageUrl? {
    val attachment = parts.filterIsInstance<MessagePart.Attachment>().firstOrNull() ?: return null
    val source = attachment.source
    require(source is AttachmentSource.Image) {
        "Only image attachments are supported"
    }
    val content = source.content
    require(content is AttachmentContent.URL) {
        "Only URL images are supported"
    }
    return ImageUrl(content.url)
}

fun ApiAction.createEvent(): Event =
    when (this) {
        is ApiUserJoinedChat -> {
            Event.UserJoined(EventId.random(), Clock.System.now(), joinId)
        }

        is ApiUserSendsMessage -> {
            val parts =
                buildList {
                    text?.let { add(MessagePart.Text(it)) }
                    imageUrl?.let { imageUrl ->
                        val format =
                            imageUrl.value
                                .substringAfterLast('.')
                                .substringBefore('?')
                                .ifEmpty { "jpeg" }
                        add(
                            MessagePart.Attachment(
                                AttachmentSource.Image(AttachmentContent.URL(imageUrl.value), format),
                            ),
                        )
                    }
                }
            val koogMessage = Message.User(parts, RequestMetaInfo(Clock.System.now()))
            Event.Message(EventId.random(), koogMessage)
        }
    }
