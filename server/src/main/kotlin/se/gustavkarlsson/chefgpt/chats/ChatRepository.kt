package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.auth.UserId

interface ChatRepository {
    suspend fun create(userId: UserId): Chat

    suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean

    suspend fun getAll(userId: UserId): List<Chat>

    suspend operator fun get(
        userId: UserId,
        chatId: ChatId,
    ): Chat? = getAll(userId).find { it.id == chatId }

    suspend fun contains(
        userId: UserId,
        chatId: ChatId,
    ): Boolean = chatId in getAll(userId).map(Chat::id)
}
