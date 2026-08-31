package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock

class InMemoryChatRepository : ChatRepository {
    private val storage = ConcurrentHashMap<UserId, MutableStateFlow<List<Chat>>>()

    override suspend fun contains(
        userId: UserId,
        chatId: ChatId,
    ): Boolean = chatId in storage[userId]?.value.orEmpty().map { it.id }

    override suspend operator fun get(
        userId: UserId,
        chatId: ChatId,
    ): Chat? = storage[userId]?.value.orEmpty().find { it.id == chatId }

    override suspend fun create(userId: UserId): Chat {
        val chat = Chat(ChatId.random(), Clock.System.now(), name = null)
        storage.getOrPut(userId) { MutableStateFlow(emptyList()) }.update { it + chat }
        return chat
    }

    override fun stream(userId: UserId): Flow<List<Chat>> =
        storage.getOrPut(userId) { MutableStateFlow(emptyList()) }.asSharedFlow()

    override suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean {
        val flow = storage[userId] ?: return false
        var removed = false
        flow.update { chats ->
            val filtered = chats.filter { it.id != chatId }
            removed = filtered.size < chats.size
            filtered
        }
        return removed
    }

    override suspend fun rename(
        userId: UserId,
        chatId: ChatId,
        name: String,
    ): Boolean {
        val flow = storage[userId] ?: return false
        var renamed = false
        flow.update { chats ->
            chats.map { chat ->
                if (chat.id == chatId) {
                    renamed = true
                    chat.copy(name = name)
                } else {
                    chat
                }
            }
        }
        return renamed
    }
}
