package se.gustavkarlsson.chefgpt.chats

import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ImageUrl
import ai.koog.prompt.message.Message as KoogMessage

private fun KoogMessage.User.getImageUrl(): ImageUrl? {
    val imagePart = parts.filterIsInstance<ContentPart.Image>().firstOrNull() ?: return null
    val content = imagePart.content
    require(content is AttachmentContent.URL) {
        "Only URL images are supported"
    }
    return ImageUrl(content.url)
}

fun Event.toApi(): ApiEvent {
    TODO()
}

fun ApiAction.toEvent(): Event {
    TODO()
}
