package se.gustavkarlsson.chefgpt.chats
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiUploadedFile
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.files.HtmlLoader
import se.gustavkarlsson.chefgpt.files.UploadedFile
import kotlin.test.Test
import kotlin.test.assertEquals

private val image =
    ApiUploadedFile("https://res.cloudinary.com/demo/image/upload/v1/page.jpg", "image/jpeg", "page.jpg")
private val pdf =
    ApiUploadedFile("https://res.cloudinary.com/demo/image/upload/v1/recipe.pdf", "application/pdf", "recipe.pdf")
private val text =
    ApiUploadedFile("https://res.cloudinary.com/demo/raw/upload/v1/recipe.txt", "text/plain", "recipe.txt")

class CreateEventTest {
    private val htmlLoader =
        object : HtmlLoader {
            var result: String? = "Boil water"

            override suspend fun loadText(url: String): String? = result
        }

    @Test
    fun `sends an image by url`() =
        runTest {
            val event = ApiUserSendsMessage("Look", listOf(image)).toEvent(htmlLoader)

            assertEquals(
                listOf(
                    TextPart("Look", cacheControl = null),
                    AttachmentPart(
                        AttachmentSource.Image(AttachmentContent.Url(image.url), "jpg", "image/jpeg", "page.jpg"),
                        cacheControl = null,
                    ),
                ),
                (event as Event.Message).message.let { (it as ChatMessage.User).parts },
            )
        }

    @Test
    fun `sends a pdf by url`() =
        runTest {
            val event = ApiUserSendsMessage(null, listOf(pdf)).toEvent(htmlLoader)

            assertEquals(
                listOf(
                    AttachmentPart(
                        AttachmentSource.File(
                            AttachmentContent.Url(pdf.url),
                            "pdf",
                            "application/pdf",
                            "recipe.pdf",
                        ),
                        cacheControl = null,
                    ),
                ),
                (event as Event.Message).message.let { (it as ChatMessage.User).parts },
            )
        }

    @Test
    fun `inlines the content of a text file`() =
        runTest {
            val event = ApiUserSendsMessage(null, listOf(text)).toEvent(htmlLoader)

            assertEquals(
                listOf(
                    AttachmentPart(
                        AttachmentSource.File(
                            AttachmentContent.PlainText("Boil water"),
                            "txt",
                            "text/plain",
                            "recipe.txt",
                        ),
                        cacheControl = null,
                    ),
                ),
                (event as Event.Message).message.let { (it as ChatMessage.User).parts },
            )
        }

    @Test
    fun `leaves out a text file it could not read`() =
        runTest {
            htmlLoader.result = null

            val event = ApiUserSendsMessage("Look", listOf(text)).toEvent(htmlLoader)

            assertEquals(
                listOf(TextPart("Look", cacheControl = null)),
                (event as Event.Message).message.let { (it as ChatMessage.User).parts },
            )
        }

    @Test
    fun `keeps the attachments so the user still sees what they shared`() =
        runTest {
            htmlLoader.result = null

            val event = ApiUserSendsMessage("Look", listOf(text)).toEvent(htmlLoader)

            assertEquals(
                listOf(
                    UploadedFile(
                        "https://res.cloudinary.com/demo/raw/upload/v1/recipe.txt",
                        "text/plain",
                        "recipe.txt",
                    ),
                ),
                (event as Event.Message).attachments,
            )
        }
}
