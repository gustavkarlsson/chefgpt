package se.gustavkarlsson.chefgpt.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.files.sharedAttachments
import se.gustavkarlsson.chefgpt.toDomain

/**
 * Hands the chat agent the urls of the files the user shared, so it can pass photo urls on to
 * the image scanning tools. The chat agent is not shown photos in its own prompt.
 */
class ListSharedFilesTool(
    private val eventRepository: EventRepository,
    private val chatId: ChatId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "List the files the user has uploaded in this chat, in the order they uploaded them. " +
            "You cannot see photos yourself, so use this to get their url:s and hand them to the " +
            "scanning tools (addRecipesFromPhotos, addIngredientsFromPhotos, describePhotos).",
    )
    suspend fun listSharedFiles(): List<UploadedFile> =
        eventRepository.sharedAttachments(chatId).map { attachment ->
            attachment.toDomain()
        }
}
