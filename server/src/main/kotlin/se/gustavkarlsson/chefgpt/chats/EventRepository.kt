package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId

interface EventRepository {
    suspend fun append(
        chatId: ChatId,
        event: Event,
    )

    suspend fun getAll(chatId: ChatId): List<Event>

    fun flow(
        chatId: ChatId,
        last: EventId? = null,
    ): Flow<Event>
}
