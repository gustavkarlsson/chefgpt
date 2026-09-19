package se.gustavkarlsson.chefgpt.files

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiUploadedFile
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

private const val PAGE = "https://res.cloudinary.com/demo/image/upload/v123/page.jpg"
private const val DISH = "https://res.cloudinary.com/demo/image/upload/v123/dish.jpg"
private const val NOTES = "https://res.cloudinary.com/demo/raw/upload/v123/notes.txt"

class UploadedFileToolsTest {
    private val chatId = ChatId.random()
    private val eventRepository = InMemoryEventRepository()
    private val tools = UploadedFileTools(eventRepository, chatId)

    private suspend fun share(vararg attachments: ApiUploadedFile) {
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
    fun `lists the shared files in the order they were shared`() =
        runTest {
            share(ApiUploadedFile(PAGE, "image/jpeg", "page.jpg"))
            share(ApiUploadedFile(NOTES, "text/plain", "notes.txt"), ApiUploadedFile(DISH, "image/jpeg", "dish.jpg"))

            assertEquals(
                listOf(
                    UploadedFile(PAGE, "image/jpeg", "page.jpg"),
                    UploadedFile(NOTES, "text/plain", "notes.txt"),
                    UploadedFile(DISH, "image/jpeg", "dish.jpg"),
                ),
                tools.listSharedFiles(),
            )
        }

    @Test
    fun `has nothing to list before anything is shared`() =
        runTest {
            assertEquals(emptyList(), tools.listSharedFiles())
        }
}
