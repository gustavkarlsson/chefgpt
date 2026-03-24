package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentStorageKey
import ai.koog.agents.core.feature.AIAgentGraphFeature
import ai.koog.agents.core.feature.config.FeatureConfig
import ai.koog.agents.core.feature.pipeline.AIAgentGraphPipeline
import ai.koog.prompt.message.Message
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.chats.Chat
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.Event
import java.util.concurrent.atomic.AtomicReference
import kotlin.uuid.Uuid

class EventBackedChatMemory {
    companion object Feature :
        AIAgentGraphFeature<ChatMemoryConfig, EventBackedChatMemory> {
        override val key: AIAgentStorageKey<EventBackedChatMemory> =
            AIAgentStorageKey("agents-features-event-backed-chat-memory")

        override fun createInitialConfig(agentConfig: AIAgentConfig): ChatMemoryConfig = ChatMemoryConfig()

        override fun install(
            config: ChatMemoryConfig,
            pipeline: AIAgentGraphPipeline,
        ): EventBackedChatMemory {
            val chatMemory = EventBackedChatMemory()
            val chatRepository =
                requireNotNull(config.chatRepository) {
                    "chatRepository must be set during plugin installation"
                }
            installInternal(chatRepository, pipeline)
            return chatMemory
        }

        private fun installInternal(
            chatRepository: ChatRepository,
            pipeline: AIAgentGraphPipeline,
        ) {
            val lastMessage = AtomicReference<Message?>(null)

            pipeline.interceptStrategyStarting(this) { stragegyStarting ->
                val chat = chatRepository.requireChat(stragegyStarting.context.runId)
                stragegyStarting.context.llm.writeSession {
                    for (event in chat.events().replayCache.filterIsInstance<Event.Message>()) {
                        appendPrompt {
                            message(event.message)
                        }
                        lastMessage.set(event.message)
                    }
                }
            }

            pipeline.interceptNodeExecutionCompleted(this) { executionCompleted ->
                val messagesSinceLast =
                    executionCompleted.context.llm.prompt.messages
                        .takeLastWhile { it != lastMessage.get() }
                if (messagesSinceLast.isNotEmpty()) {
                    val chat = chatRepository.requireChat(executionCompleted.context.runId)
                    for (message in messagesSinceLast) {
                        val event = Event.Message(Uuid.random(), message)
                        chat.append(event)
                    }
                }
            }
            // TODO do we need to intercept failed nodes too?
        }
    }
}

private suspend fun ChatRepository.requireChat(runId: String): Chat {
    val chatId = ChatId.parse(runId)
    val chat = this[chatId] ?: throw NoSuchElementException("No chat with ID $chatId found")
    return chat
}

class ChatMemoryConfig : FeatureConfig() {
    var chatRepository: ChatRepository? = null
}
