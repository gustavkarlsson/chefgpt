package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId

interface ChatRepository {
    suspend fun create(userId: UserId): Chat

    suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean

    fun stream(userId: UserId): Flow<List<Chat>>

    suspend operator fun get(
        userId: UserId,
        chatId: ChatId,
    ): Chat?

    suspend fun contains(
        userId: UserId,
        chatId: ChatId,
    ): Boolean
}
