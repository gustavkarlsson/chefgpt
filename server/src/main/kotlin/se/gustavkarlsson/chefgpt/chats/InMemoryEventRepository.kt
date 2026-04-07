package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import java.util.concurrent.ConcurrentHashMap

class InMemoryEventRepository(
    private val flowsByChatId: ConcurrentHashMap<ChatId, MutableSharedFlow<Event>> = ConcurrentHashMap(),
) : EventRepository {
    private fun getOrCreateFlow(chatId: ChatId): MutableSharedFlow<Event> =
        flowsByChatId.computeIfAbsent(chatId) { MutableSharedFlow(replay = Int.MAX_VALUE) }

    override suspend fun append(
        chatId: ChatId,
        event: Event,
    ) {
        val flow = getOrCreateFlow(chatId)
        flow.emit(event)
    }

    override suspend fun getAll(chatId: ChatId): List<Event> = getOrCreateFlow(chatId).replayCache

    override fun flow(
        chatId: ChatId,
        last: EventId?,
    ): Flow<Event> {
        val flow = getOrCreateFlow(chatId)
        if (last == null) return flow
        return flow.dropWhile { event -> event.id != last }.drop(1)
    }
}
