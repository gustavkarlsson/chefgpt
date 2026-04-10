package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryChatRepositoryTest {
    private val userId = UserId.random()
    private val otherUserId = UserId.random()
    private val repo = InMemoryChatRepository()

    @Test
    fun `create returns a new chat`() =
        runTest {
            val chat = repo.create(userId)

            assertNotNull(chat)
        }

    @Test
    fun `create assigns distinct ids to each chat`() =
        runTest {
            val chat1 = repo.create(userId)
            val chat2 = repo.create(userId)

            assertNotEquals(chat1.id, chat2.id)
        }

    @Test
    fun `get returns the created chat`() =
        runTest {
            val chat = repo.create(userId)

            val result = repo.get(userId, chat.id)

            assertEquals(chat, result)
        }

    @Test
    fun `get returns null for non-existent chat id`() =
        runTest {
            val result = repo.get(userId, ChatId.random())

            assertNull(result)
        }

    @Test
    fun `get returns null when chat belongs to another user`() =
        runTest {
            val chat = repo.create(userId)

            val result = repo.get(otherUserId, chat.id)

            assertNull(result)
        }

    @Test
    fun `contains returns true for an existing chat`() =
        runTest {
            val chat = repo.create(userId)

            assertTrue(repo.contains(userId, chat.id))
        }

    @Test
    fun `contains returns false for a non-existent chat id`() =
        runTest {
            val result = repo.contains(userId, ChatId.random())

            assertFalse(result)
        }

    @Test
    fun `contains returns false when chat belongs to another user`() =
        runTest {
            val chat = repo.create(userId)

            assertFalse(repo.contains(otherUserId, chat.id))
        }

    @Test
    fun `delete returns true when chat exists`() =
        runTest {
            val chat = repo.create(userId)

            val result = repo.delete(userId, chat.id)

            assertTrue(result)
        }

    @Test
    fun `delete removes the chat`() =
        runTest {
            val chat = repo.create(userId)
            repo.delete(userId, chat.id)

            assertNull(repo.get(userId, chat.id))
        }

    @Test
    fun `delete returns false when chat does not exist`() =
        runTest {
            val result = repo.delete(userId, ChatId.random())

            assertFalse(result)
        }

    @Test
    fun `delete returns false when chat belongs to another user`() =
        runTest {
            val chat = repo.create(userId)

            val result = repo.delete(otherUserId, chat.id)

            assertFalse(result)
        }

    @Test
    fun `delete does not affect another user's chat`() =
        runTest {
            val chat = repo.create(userId)
            repo.delete(otherUserId, chat.id)

            assertNotNull(repo.get(userId, chat.id))
        }

    @Test
    fun `stream emits empty list for new user`() =
        runTest {
            val chats = repo.stream(userId).first()

            assertTrue(chats.isEmpty())
        }

    @Test
    fun `stream emits current list of chats`() =
        runTest {
            val chat = repo.create(userId)

            val chats = repo.stream(userId).first()

            assertEquals(listOf(chat), chats)
        }

    @Test
    fun `stream is independent per user`() =
        runTest {
            repo.create(userId)

            val otherChats = repo.stream(otherUserId).first()

            assertTrue(otherChats.isEmpty())
        }
}
