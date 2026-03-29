package se.gustavkarlsson.chefgpt.chats

import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.api.ChatId
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

private val logger = LoggerFactory.getLogger(EventRepository::class.java)

class InMemoryEventRepository : EventRepository {
    private val flowsByChatId = ConcurrentHashMap<ChatId, MutableSharedFlow<Event>>()

    private fun getOrCreateFlow(chatId: ChatId): MutableSharedFlow<Event> =
        flowsByChatId.computeIfAbsent(chatId) { MutableSharedFlow(replay = Int.MAX_VALUE) }

    override suspend fun append(
        chatId: ChatId,
        event: Event,
    ) {
        val flow = getOrCreateFlow(chatId)
        logger.info("Event: {}", event.truncateContent())
        flow.emit(event)
    }

    override suspend fun list(
        chatId: ChatId,
        fromId: Uuid?,
        toId: Uuid?,
    ): List<Event> {
        val events = getOrCreateFlow(chatId).replayCache
        val fromIndex =
            if (fromId != null) {
                val index = events.indexOfFirst { it.id == fromId }
                if (index == -1) return emptyList() else index
            } else {
                0
            }
        val toIndex =
            if (toId != null) {
                val index = events.indexOfFirst { it.id == toId }
                if (index == -1) return emptyList() else index + 1
            } else {
                events.size
            }
        if (fromIndex > toIndex) return emptyList()
        return events.subList(fromIndex, toIndex)
    }

    override fun flow(
        chatId: ChatId,
        last: Uuid?,
    ): Flow<Event> {
        val flow = getOrCreateFlow(chatId)
        if (last == null) return flow
        return flow.dropWhile { event -> event.id != last }.drop(1)
    }
}

private fun Event.truncateContent(): Event =
    if (this is Event.Message) {
        val truncatedParts =
            message.parts.map { part ->
                if (part is ContentPart.Text) {
                    val truncatedText =
                        if (part.text.length > 50) {
                            part.text.take(47) + "..."
                        } else {
                            part.text
                        }
                    part.copy(text = truncatedText)
                } else {
                    part
                }
            }
        val message =
            when (val message = message) {
                is Message.User -> message.copy(parts = truncatedParts)
                is Message.Assistant -> message.copy(parts = truncatedParts)
                is Message.System -> message.copy(parts = truncatedParts.filterIsInstance<ContentPart.Text>())
                is Message.Tool.Result -> message.copy(parts = truncatedParts.filterIsInstance<ContentPart.Text>())
                is Message.Reasoning -> message.copy(parts = truncatedParts.filterIsInstance<ContentPart.Text>())
                is Message.Tool.Call -> message.copy(parts = truncatedParts.filterIsInstance<ContentPart.Text>())
            }
        this.copy(message = message)
    } else {
        this
    }
