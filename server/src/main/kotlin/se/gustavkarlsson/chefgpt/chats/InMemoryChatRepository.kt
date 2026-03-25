package se.gustavkarlsson.chefgpt.chats

import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId

private val logger = LoggerFactory.getLogger(ChatRepository::class.java)

class InMemoryChatRepository : ChatRepository {
    private val chatIdsByUserId = mutableMapOf<UserId, MutableList<ChatId>>()
    private val chatsByChatId = mutableMapOf<ChatId, Chat>()
    private val mutex = Mutex()

    override suspend fun contains(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        mutex.withLock {
            val userChatIds = chatIdsByUserId[userId].orEmpty()
            chatId in userChatIds
        }

    override suspend operator fun get(
        userId: UserId,
        chatId: ChatId,
    ): Chat? =
        mutex.withLock {
            val userChatIds = chatIdsByUserId[userId].orEmpty()
            if (chatId in userChatIds) {
                chatsByChatId.getValue(chatId)
            } else {
                null
            }
        }

    override suspend operator fun get(chatId: ChatId): Chat? =
        mutex.withLock {
            chatsByChatId[chatId]
        }

    override suspend fun create(userId: UserId): Chat =
        mutex.withLock {
            val chat = InMemoryChat(ChatId.random())
            chatIdsByUserId.getOrPut(userId) { mutableListOf() }.add(chat.id)
            chatsByChatId[chat.id] = chat
            chat
        }

    override suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        mutex.withLock {
            chatIdsByUserId[userId]?.remove(chatId)
            chatsByChatId.remove(chatId) != null
        }

    override suspend fun getAll(userId: UserId): List<Chat> {
        val userChatIds = chatIdsByUserId[userId].orEmpty()
        return chatsByChatId.filterKeys(userChatIds::contains).values.toList()
    }
}

private class InMemoryChat(
    override val id: ChatId,
) : Chat {
    private val flow = MutableSharedFlow<Event>(replay = Int.MAX_VALUE)

    override suspend fun append(event: Event) {
        logger.info("Event: {}", event.truncate())
        flow.emit(event)
    }

    private fun Event.truncate(): Event =
        if (this is Event.Message) {
            val truncatedParts =
                message.parts.map { part ->
                    if (part is ContentPart.Text) {
                        part.copy(text = part.text.take(50))
                    } else {
                        part
                    }
                }
            val message =
                when (val message = message) {
                    is Message.User -> message
                    is Message.Assistant -> message
                    is Message.System -> message.copy(parts = truncatedParts.filterIsInstance<ContentPart.Text>())
                    is Message.Tool.Result -> message.copy(parts = truncatedParts.filterIsInstance<ContentPart.Text>())
                    is Message.Reasoning -> message.copy(parts = truncatedParts.filterIsInstance<ContentPart.Text>())
                    is Message.Tool.Call -> message.copy(parts = truncatedParts.filterIsInstance<ContentPart.Text>())
                }
            this.copy(message = message)
        } else {
            this
        }

    override suspend fun events(): SharedFlow<Event> = flow.asSharedFlow()
}
