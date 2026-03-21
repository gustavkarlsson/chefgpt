package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import se.gustavkarlsson.chefgpt.api.Event
import se.gustavkarlsson.chefgpt.api.Message
import java.util.concurrent.ConcurrentHashMap

class EventFlowManager(
    private val loadMessageHistory: suspend (ChatId) -> List<Message>,
) {
    private val refCounts = ConcurrentHashMap<ChatId, Int>()
    private val mutexes = mutableMapOf<ChatId, Mutex>()
    private val flows = mutableMapOf<ChatId, Deferred<MutableSharedFlow<Event>>>()

    suspend fun <T> use(
        chatId: ChatId,
        use: suspend (MutableSharedFlow<Event>) -> T,
    ): T =
        coroutineScope {
            try {
                val deferredFlow = startUse(chatId)
                use(deferredFlow.await())
            } finally {
                stopUse(chatId)
            }
        }

    @Synchronized
    private fun CoroutineScope.startUse(chatId: ChatId): Deferred<MutableSharedFlow<Event>> {
        val newCount = refCounts.compute(chatId) { _, count -> (count ?: 0) + 1 }
        return if (newCount == 1) {
            // First use of this chatId. Populate maps.
            require(mutexes.put(chatId, Mutex()) == null) {
                "A mutex already exists for first use of chat $chatId"
            }
            val deferredFlow =
                async {
                    createChatEventFlowFromHistory(chatId)
                }
            require(flows.put(chatId, deferredFlow) == null) {
                "A flow already exists for first use of chat $chatId"
            }
            deferredFlow
        } else {
            flows.getValue(chatId)
        }
    }

    private suspend fun createChatEventFlowFromHistory(chatId: ChatId): MutableSharedFlow<Event> {
        val flow = MutableSharedFlow<Event>(replay = Int.MAX_VALUE)
        for (message in loadMessageHistory(chatId)) {
            flow.emit(Event.Message(message))
        }
        return flow
    }

    @Synchronized
    private fun stopUse(chatId: ChatId) {
        val newCount = refCounts.compute(chatId) { _, count -> (count ?: 0) - 1 }
        if (newCount == 0) {
            // Last use of this chatId. Clean up.
            requireNotNull(refCounts.remove(chatId)) {
                "No ref count exists for chat $chatId"
            }
            requireNotNull(mutexes.remove(chatId)) {
                "No mutex exists for chat $chatId"
            }
            requireNotNull(flows.remove(chatId)) {
                "No flow exists for chat $chatId"
            }
        }
    }
}
