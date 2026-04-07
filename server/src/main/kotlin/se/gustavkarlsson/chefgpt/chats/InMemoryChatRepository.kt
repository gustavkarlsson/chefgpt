package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Clock

class InMemoryChatRepository : ChatRepository {
    private val storage = ConcurrentHashMap<UserId, CopyOnWriteArrayList<Chat>>()

    override suspend fun contains(
        userId: UserId,
        chatId: ChatId,
    ): Boolean = chatId in storage[userId].orEmpty().map { it.id }

    override suspend operator fun get(
        userId: UserId,
        chatId: ChatId,
    ): Chat? = storage[userId].orEmpty().find { it.id == chatId }

    override suspend fun create(userId: UserId): Chat {
        val chat = Chat(ChatId.random(), Clock.System.now())
        storage.getOrDefault(userId, CopyOnWriteArrayList()).add(chat)
        return chat
    }

    override suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean = storage[userId]?.removeIf { it.id == chatId } == true

    override suspend fun getAll(userId: UserId): List<Chat> = storage[userId].orEmpty().toList()
}
