package se.gustavkarlsson.chefgpt.files

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock

private const val PAGE = "https://res.cloudinary.com/demo/image/upload/v123/page.jpg"
private const val DISH = "https://res.cloudinary.com/demo/image/upload/v123/dish.jpg"
private const val NOTES = "https://res.cloudinary.com/demo/raw/upload/v123/notes.txt"

class SharedFileToolsTest {
    private val chatId = ChatId.random()
    private val eventRepository = InMemoryEventRepository()
    private val tools = SharedFileTools(eventRepository, CloudinaryImageCropper("demo"), chatId)

    private suspend fun share(vararg attachments: ApiAttachment) {
        eventRepository.append(
            chatId,
            Event.Message(
                id = EventId.random(),
                message = Message.User(listOf(MessagePart.Text("Look")), RequestMetaInfo(Clock.System.now())),
                attachments = attachments.toList(),
            ),
        )
    }

    @Test
    fun `numbers the shared files in the order they were shared`() =
        runTest {
            share(ApiAttachment(PAGE, "image/jpeg", "page.jpg"))
            share(ApiAttachment(NOTES, "text/plain", "notes.txt"), ApiAttachment(DISH, "image/jpeg", "dish.jpg"))

            assertEquals(
                listOf(
                    SharedFile(1, PAGE, "image", "page.jpg"),
                    SharedFile(2, NOTES, "text", "notes.txt"),
                    SharedFile(3, DISH, "image", "dish.jpg"),
                ),
                tools.listSharedFiles(),
            )
        }

    @Test
    fun `has nothing to list before anything is shared`() =
        runTest {
            assertEquals(emptyList(), tools.listSharedFiles())
        }

    @Test
    fun `crops a shared picture`() =
        runTest {
            share(ApiAttachment(PAGE, "image/jpeg", "page.jpg"))

            assertEquals(
                "https://res.cloudinary.com/demo/image/upload/" +
                    "c_crop,x_0.0000,y_0.0000,w_0.9999,h_0.3300/v123/page.jpg",
                tools.cropImage(PAGE, x = 0.0, y = 0.0, width = 1.0, height = 0.33),
            )
        }

    @Test
    fun `refuses to crop a picture that was not shared here`() =
        runTest {
            share(ApiAttachment(PAGE, "image/jpeg", "page.jpg"))

            assertFailsWith<IllegalArgumentException> {
                tools.cropImage(DISH, x = 0.0, y = 0.0, width = 0.5, height = 0.5)
            }
        }

    @Test
    fun `refuses to crop a shared file that is not a picture`() =
        runTest {
            share(ApiAttachment(NOTES, "text/plain", "notes.txt"))

            assertFailsWith<IllegalArgumentException> {
                tools.cropImage(NOTES, x = 0.0, y = 0.0, width = 0.5, height = 0.5)
            }
        }

    @Test
    fun `says so when the region is not inside the picture`() =
        runTest {
            share(ApiAttachment(PAGE, "image/jpeg", "page.jpg"))

            assertFailsWith<IllegalStateException> {
                tools.cropImage(PAGE, x = 0.8, y = 0.0, width = 0.5, height = 0.5)
            }
        }
}
