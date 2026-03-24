package se.gustavkarlsson.chefgpt.chats

import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import se.gustavkarlsson.chefgpt.api.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.ApiAgentReasoning
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoined
import se.gustavkarlsson.chefgpt.api.ApiUserMessage
import se.gustavkarlsson.chefgpt.api.ImageUrl
import kotlin.time.Instant
import kotlin.uuid.Uuid
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
    id: Uuid,
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
