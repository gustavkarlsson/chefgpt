package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId

class InMemoryChatRepository : ChatRepository {
    private val chats = mutableMapOf<UserId, List<Chat>>()
    private val mutex = Mutex()

    override suspend fun create(userId: UserId): Chat =
        mutex.withLock {
            val chat = Chat(ChatId.random())
            chats.merge(userId, listOf(chat)) { chats, newChats -> chats + newChats }
            chat
        }

    override suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        mutex.withLock {
            val exists = getAll(userId).any { it.id == chatId }
            if (exists) {
                chats.computeIfPresent(userId) { _, chats -> chats.filterNot { it.id == chatId } }
                true
            } else {
                false
            }
        }

    override suspend fun getAll(userId: UserId): List<Chat> = chats[userId].orEmpty()
}
