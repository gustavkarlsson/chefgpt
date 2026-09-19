package se.gustavkarlsson.chefgpt.files

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.agent.toDomain
import se.gustavkarlsson.chefgpt.api.ApiUploadedFile
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.EventRepository

@Serializable
@LLMDescription("A file the user has uploaded.")
data class UploadedFile(
    @property:LLMDescription("The url of the file")
    val url: String,
    @property:LLMDescription("The mime type of this file, e.g. image/png")
    val mimeType: String,
    @property:LLMDescription("The name the file had on the user's device, if it had one.")
    val fileName: String?,
)

suspend fun EventRepository.sharedAttachments(chatId: ChatId): List<ApiUploadedFile> =
    getAll(chatId)
        .filterIsInstance<Event.Message>()
        .flatMap { it.attachments }

/**
 * Hands the chat agent the urls of the files the user shared, so it can pass photo urls on to
 * the image scanning tools. The chat agent is not shown photos in its own prompt.
 */
@Suppress("unused")
class UploadedFileTools(
    private val eventRepository: EventRepository,
    private val chatId: ChatId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "List the files the user has shared in this chat, in the order they shared them. " +
            "You cannot see photos yourself, so use this to get their urls and hand them to the " +
            "scanning tools (scanRecipesInPhotos, scanIngredientsInPhotos, describePhotos).",
    )
    suspend fun listSharedFiles(): List<UploadedFile> =
        eventRepository.sharedAttachments(chatId).map { attachment ->
            attachment.toDomain()
        }
}
