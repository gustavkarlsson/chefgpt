package se.gustavkarlsson.chefgpt.agent.tools

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.agent.tools.models.ToolUploadedFile
import se.gustavkarlsson.chefgpt.api.common.ChatId
import se.gustavkarlsson.chefgpt.api.common.EventId
import se.gustavkarlsson.chefgpt.api.files.v1.ApiUploadedFile
import se.gustavkarlsson.chefgpt.chats.ChatMessage
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import se.gustavkarlsson.chefgpt.chats.TextPart
import se.gustavkarlsson.chefgpt.files.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

private const val PAGE = "https://res.cloudinary.com/demo/image/upload/v123/page.jpg"
private const val DISH = "https://res.cloudinary.com/demo/image/upload/v123/dish.jpg"
private const val NOTES = "https://res.cloudinary.com/demo/raw/upload/v123/notes.txt"

class UploadedFileToolsTest {
    private val chatId = ChatId.random()
    private val eventRepository = InMemoryEventRepository()
    private val tools = ListSharedFilesTool(eventRepository, chatId)

    private suspend fun share(vararg attachments: ApiUploadedFile) {
        val now = Clock.System.now()
        eventRepository.append(
            chatId,
            Event.Message(
                id = EventId.random(),
                message =
                    ChatMessage.User(
                        id = null,
                        timestamp = now,
                        metadata = null,
                        parts = listOf(TextPart("Look", cacheControl = null)),
                    ),
                attachments = attachments.map { it.toDomain() },
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
                    ToolUploadedFile(PAGE, "image/jpeg", "page.jpg"),
                    ToolUploadedFile(NOTES, "text/plain", "notes.txt"),
                    ToolUploadedFile(DISH, "image/jpeg", "dish.jpg"),
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
