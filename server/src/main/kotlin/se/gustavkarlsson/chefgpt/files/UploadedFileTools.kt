package se.gustavkarlsson.chefgpt.files

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.agent.toDomain
import se.gustavkarlsson.chefgpt.api.ApiUploadedFile
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.ImageUrl
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
    private val cropper: ImageCropper,
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

    @Tool
    @LLMDescription(
        "Cut a picture the user shared down to the part worth keeping, such as just the finished " +
            "dish on a page that also holds text. Returns the url of the cut-down picture, which " +
            "you can use like any other picture url. The region is given as fractions of the " +
            "picture, so x 0.1 and width 0.5 keeps the half starting a tenth in from the left.",
    )
    suspend fun cropImage(
        @LLMDescription("The url of the picture to cut down, from listSharedFiles.")
        url: String,
        @LLMDescription("Left edge of the part to keep, as a fraction of the width, from 0 to 1.")
        x: Double,
        @LLMDescription("Top edge of the part to keep, as a fraction of the height, from 0 to 1.")
        y: Double,
        @LLMDescription("Width of the part to keep, as a fraction of the picture's width.")
        width: Double,
        @LLMDescription("Height of the part to keep, as a fraction of the picture's height.")
        height: Double,
    ): String {
        val shared =
            eventRepository
                .sharedAttachments(chatId)
                .firstOrNull { it.url == url && it.kind == FileKind.Image }
        requireNotNull(shared) { "No picture shared in this chat has the url $url" }
        val region =
            runCatching { CropRegion(x, y, width, height) }
                .getOrElse { error("That is not a region inside the picture: ${it.message}") }
        return cropper.crop(ImageUrl(url), region).value
    }
}
