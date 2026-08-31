package se.gustavkarlsson.chefgpt.files

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.EventRepository

@Serializable
@LLMDescription("A file the user shared in the chat.")
data class SharedFile(
    @property:LLMDescription("The position of the file among the shared files, counting from 1.")
    val number: Int,
    @property:LLMDescription("The url of the file, to pass on to other tools.")
    val url: String,
    @property:LLMDescription("What kind of file it is: image, pdf or text.")
    val type: String,
    @property:LLMDescription("The name the file had on the user's device, if it had one.")
    val fileName: String?,
)

/**
 * Attachments reach the model as image and document content blocks, so it sees the pictures but
 * never their urls. These tools hand the urls back as tool results, which the model does read.
 */
@Suppress("unused")
class SharedFileTools(
    private val eventRepository: EventRepository,
    private val cropper: ImageCropper,
    private val chatId: ChatId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "List the files the user has shared in this chat, in the order they shared them — " +
            "the same order you were shown them in. Use this to get the url of a picture you have " +
            "been shown, for example to give a recipe a photo.",
    )
    suspend fun listSharedFiles(): List<SharedFile> =
        sharedAttachments().mapIndexed { index, attachment ->
            SharedFile(
                number = index + 1,
                url = attachment.url,
                type = attachment.kind?.name?.lowercase() ?: "unknown",
                fileName = attachment.fileName,
            )
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
        val shared = sharedAttachments().firstOrNull { it.url == url && it.kind == AttachmentKind.Image }
        requireNotNull(shared) { "No picture shared in this chat has the url $url" }
        val region =
            runCatching { CropRegion(x, y, width, height) }
                .getOrElse { error("That is not a region inside the picture: ${it.message}") }
        return cropper.crop(ImageUrl(url), region).value
    }

    private suspend fun sharedAttachments() =
        eventRepository
            .getAll(chatId)
            .filterIsInstance<Event.Message>()
            .flatMap { it.attachments }
}
