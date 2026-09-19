package se.gustavkarlsson.chefgpt.agent

import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class StripImageAttachmentsTest {
    @Test
    fun `drops image attachments and keeps text and file attachments`() {
        val messages =
            listOf(
                Message.User(
                    listOf(
                        MessagePart.Text("Here is a recipe"),
                        MessagePart.Attachment(
                            AttachmentSource.Image(
                                AttachmentContent.URL("https://example.com/photo.jpg"),
                                "jpg",
                                "image/jpeg",
                                "photo.jpg",
                            ),
                        ),
                        MessagePart.Attachment(
                            AttachmentSource.File(
                                AttachmentContent.URL("https://example.com/recipe.pdf"),
                                "pdf",
                                "application/pdf",
                                "recipe.pdf",
                            ),
                        ),
                    ),
                    RequestMetaInfo(Clock.System.now()),
                ),
            )

        val stripped = stripImageAttachments(messages)

        val parts = (stripped.single() as Message.User).parts
        assertEquals(2, parts.size)
        assertEquals("Here is a recipe", (parts[0] as MessagePart.Text).text)
        assertEquals("recipe.pdf", (parts[1] as MessagePart.Attachment).source.fileName)
    }
}
