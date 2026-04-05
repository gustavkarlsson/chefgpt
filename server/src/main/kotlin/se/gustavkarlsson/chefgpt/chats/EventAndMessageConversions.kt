package se.gustavkarlsson.chefgpt.chats

import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.ApiAgentReasoning
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
            ApiUserMessage(
                id = id,
                timestamp = timestamp,
                text = content.takeIf { it.isNotBlank() },
                imageUrl = imageUrlOrNull(),
            )
        }

        is KoogMessage.Assistant -> {
            ApiAgentMessage(
                id = id,
                timestamp = timestamp,
                text = content,
            )
        }

        is KoogMessage.Reasoning -> {
            ApiAgentReasoning(
                id = id,
                timestamp = timestamp,
                text = content,
            )
        }

        is KoogMessage.System -> {
            null
        }

        is KoogMessage.Tool -> {
            null
        }
    }

private fun KoogMessage.User.imageUrlOrNull(): ImageUrl? {
    val imagePart = parts.filterIsInstance<ContentPart.Image>().firstOrNull() ?: return null
    val content = imagePart.content
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
                    text?.let { add(ContentPart.Text(it)) }
                    imageUrl?.let { imageUrl ->
                        val format =
                            imageUrl.value
                                .substringAfterLast('.')
                                .substringBefore('?')
                                .ifEmpty { "jpeg" }
                        add(ContentPart.Image(AttachmentContent.URL(imageUrl.value), format))
                    }
                }
            val koogMessage = Message.User(parts, RequestMetaInfo(Clock.System.now()))
            Event.Message(EventId.random(), koogMessage)
        }
    }
