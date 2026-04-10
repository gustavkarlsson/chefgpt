package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.api.JoinId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class InMemoryEventRepositoryTest {
    private val chatId = ChatId.random()
    private val otherChatId = ChatId.random()
    private val repo = InMemoryEventRepository()

    private fun event() =
        Event.UserJoined(
            id = EventId.random(),
            timestamp = Clock.System.now(),
            joinId = JoinId.random(),
        )

    @Test
    fun `getAll returns empty list when no events have been appended`() =
        runTest {
            val events = repo.getAll(chatId)

            assertTrue(events.isEmpty())
        }

    @Test
    fun `getAll returns all appended events`() =
        runTest {
            val event1 = event()
            val event2 = event()
            repo.append(chatId, event1)
            repo.append(chatId, event2)

            val events = repo.getAll(chatId)

            assertEquals(listOf(event1, event2), events)
        }

    @Test
    fun `getAll preserves insertion order`() =
        runTest {
            val events = List(3) { event() }
            events.forEach { repo.append(chatId, it) }

            val result = repo.getAll(chatId)

            assertEquals(events, result)
        }

    @Test
    fun `getAll is independent per chat`() =
        runTest {
            repo.append(chatId, event())

            val events = repo.getAll(otherChatId)

            assertTrue(events.isEmpty())
        }

    @Test
    fun `flow with null last emits all events from replay`() =
        runTest {
            val event1 = event()
            val event2 = event()
            repo.append(chatId, event1)
            repo.append(chatId, event2)

            val events = repo.flow(chatId, last = null).take(2).toList()

            assertEquals(listOf(event1, event2), events)
        }

    @Test
    fun `flow with last set skips events up to and including that event`() =
        runTest {
            val event1 = event()
            val event2 = event()
            val event3 = event()
            repo.append(chatId, event1)
            repo.append(chatId, event2)
            repo.append(chatId, event3)

            val events = repo.flow(chatId, last = event1.id).take(2).toList()

            assertEquals(listOf(event2, event3), events)
        }

    @Test
    fun `flow with last set to middle event returns only later events`() =
        runTest {
            val event1 = event()
            val event2 = event()
            val event3 = event()
            repo.append(chatId, event1)
            repo.append(chatId, event2)
            repo.append(chatId, event3)

            val events = repo.flow(chatId, last = event2.id).take(1).toList()

            assertEquals(listOf(event3), events)
        }

    @Test
    fun `flow is independent per chat`() =
        runTest {
            val event1 = event()
            val event2 = event()
            repo.append(chatId, event1)
            repo.append(otherChatId, event2)

            val chatEvents = repo.flow(chatId, last = null).take(1).toList()
            val otherChatEvents = repo.flow(otherChatId, last = null).take(1).toList()

            assertEquals(listOf(event1), chatEvents)
            assertEquals(listOf(event2), otherChatEvents)
        }
}
