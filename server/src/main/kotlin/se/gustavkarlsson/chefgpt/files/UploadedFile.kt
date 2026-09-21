package se.gustavkarlsson.chefgpt.files

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
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
