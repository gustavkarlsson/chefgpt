package se.gustavkarlsson.chefgpt.chats

import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import se.gustavkarlsson.chefgpt.api.AgentEvent
import se.gustavkarlsson.chefgpt.api.AgentMessage
import se.gustavkarlsson.chefgpt.api.AgentReasoning
import se.gustavkarlsson.chefgpt.api.Event
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.ToolCall
import se.gustavkarlsson.chefgpt.api.UserMessage
import ai.koog.prompt.message.Message as KoogMessage

fun KoogMessage.toEventOrNull(): Event? =
    when (this) {
        is KoogMessage.System -> null

        // System prompt is hidden
        is KoogMessage.Tool.Result -> null

        // We're not interested in tool results
        is KoogMessage.User -> UserMessage(content, getImageUrl())

        is KoogMessage.Response -> toEvent()
    }

private fun KoogMessage.User.getImageUrl(): ImageUrl? {
    val imagePart = parts.filterIsInstance<ContentPart.Image>().firstOrNull() ?: return null
    val content = imagePart.content
    require(content is AttachmentContent.URL) {
        "Only URL images are supported"
    }
    return ImageUrl(content.url)
}

fun KoogMessage.Response.toEvent(): AgentEvent =
    when (this) {
        is KoogMessage.Assistant -> AgentMessage(content)
        is KoogMessage.Reasoning -> AgentReasoning(content)
        is KoogMessage.Tool.Call -> ToolCall
    }
