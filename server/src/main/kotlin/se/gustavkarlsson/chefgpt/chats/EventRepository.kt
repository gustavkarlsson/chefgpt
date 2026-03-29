package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.api.ChatId
import kotlin.uuid.Uuid

interface EventRepository {
    suspend fun append(
        chatId: ChatId,
        event: Event,
    )

    suspend fun list(
        chatId: ChatId,
        fromId: Uuid? = null,
        toId: Uuid? = null,
    ): List<Event>

    fun flow(
        chatId: ChatId,
        last: Uuid? = null,
    ): Flow<Event>
}
