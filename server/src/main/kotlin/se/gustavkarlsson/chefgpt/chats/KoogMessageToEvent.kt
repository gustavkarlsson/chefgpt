package se.gustavkarlsson.chefgpt.chats

import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import se.gustavkarlsson.chefgpt.api.Action
import se.gustavkarlsson.chefgpt.api.AgentAction
import se.gustavkarlsson.chefgpt.api.AgentMessage
import se.gustavkarlsson.chefgpt.api.AgentReasoning
import se.gustavkarlsson.chefgpt.api.AgentToolCall
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.UserMessage
import ai.koog.prompt.message.Message as KoogMessage

fun KoogMessage.toActionOrNull(): Action? =
    when (this) {
        // System prompt is hidden
        is KoogMessage.System -> null

        // We're not interested in tool results
        is KoogMessage.Tool.Result -> null

        is KoogMessage.User -> UserMessage(content, getImageUrl())

        is KoogMessage.Response -> toAgentAction()
    }

private fun KoogMessage.User.getImageUrl(): ImageUrl? {
    val imagePart = parts.filterIsInstance<ContentPart.Image>().firstOrNull() ?: return null
    val content = imagePart.content
    require(content is AttachmentContent.URL) {
        "Only URL images are supported"
    }
    return ImageUrl(content.url)
}

fun KoogMessage.Response.toAgentAction(): AgentAction =
    when (this) {
        is KoogMessage.Assistant -> AgentMessage(content)
        is KoogMessage.Reasoning -> AgentReasoning(content)
        is KoogMessage.Tool.Call -> AgentToolCall
    }
