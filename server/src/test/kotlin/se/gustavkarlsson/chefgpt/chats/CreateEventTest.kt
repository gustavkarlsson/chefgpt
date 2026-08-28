package se.gustavkarlsson.chefgpt.chats

import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.files.AttachmentTextLoader
import kotlin.test.Test
import kotlin.test.assertEquals

private val image =
    ApiAttachment("https://res.cloudinary.com/demo/image/upload/v1/page.jpg", "image/jpeg", "page.jpg")
private val pdf =
    ApiAttachment("https://res.cloudinary.com/demo/image/upload/v1/recipe.pdf", "application/pdf", "recipe.pdf")
private val text =
    ApiAttachment("https://res.cloudinary.com/demo/raw/upload/v1/recipe.txt", "text/plain", "recipe.txt")

class CreateEventTest {
    private val textLoader =
        object : AttachmentTextLoader {
            var result: String? = "Boil water"

            override suspend fun loadText(url: String): String? = result
        }

    @Test
    fun `sends an image by url`() =
        runTest {
            val event = ApiUserSendsMessage("Look", listOf(image)).createEvent(textLoader)

            assertEquals(
                listOf(
                    MessagePart.Text("Look"),
                    MessagePart.Attachment(
                        AttachmentSource.Image(
                            AttachmentContent.URL(image.url),
                            "jpg",
                            "image/jpeg",
                            "page.jpg",
                        ),
                    ),
                ),
                (event as Event.Message).message.let { (it as Message.User).parts },
            )
        }

    @Test
    fun `sends a pdf by url`() =
        runTest {
            val event = ApiUserSendsMessage(null, listOf(pdf)).createEvent(textLoader)

            assertEquals(
                listOf(
                    MessagePart.Attachment(
                        AttachmentSource.File(
                            AttachmentContent.URL(pdf.url),
                            "pdf",
                            "application/pdf",
                            "recipe.pdf",
                        ),
                    ),
                ),
                (event as Event.Message).message.let { (it as Message.User).parts },
            )
        }

    @Test
    fun `inlines the content of a text file`() =
        runTest {
            val event = ApiUserSendsMessage(null, listOf(text)).createEvent(textLoader)

            assertEquals(
                listOf(
                    MessagePart.Attachment(
                        AttachmentSource.File(
                            AttachmentContent.PlainText("Boil water"),
                            "txt",
                            "text/plain",
                            "recipe.txt",
                        ),
                    ),
                ),
                (event as Event.Message).message.let { (it as Message.User).parts },
            )
        }

    @Test
    fun `leaves out a text file it could not read`() =
        runTest {
            textLoader.result = null

            val event = ApiUserSendsMessage("Look", listOf(text)).createEvent(textLoader)

            assertEquals(
                listOf(MessagePart.Text("Look")),
                (event as Event.Message).message.let { (it as Message.User).parts },
            )
        }

    @Test
    fun `keeps the attachments so the user still sees what they shared`() =
        runTest {
            textLoader.result = null

            val event = ApiUserSendsMessage("Look", listOf(text)).createEvent(textLoader)

            assertEquals(listOf(text), (event as Event.Message).attachments)
        }
}
