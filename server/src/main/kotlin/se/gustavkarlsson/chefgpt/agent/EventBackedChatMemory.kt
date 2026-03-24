package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.context.AIAgentGraphContextBase
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
            val lastMessageHolder = AtomicReference<Message?>(null)

            pipeline.interceptStrategyStarting(this) {
                it.context.writeOldChatMessagesToPrompt(chatRepository, lastMessageHolder)
            }

            // TODO It takes quite a while for the user message to land. Should we send it manually somewhere else and remove this?
            pipeline.interceptNodeExecutionStarting(this) {
                it.context.writeNewPromptMessagesToChat(chatRepository, lastMessageHolder)
            }

            pipeline.interceptNodeExecutionCompleted(this) {
                it.context.writeNewPromptMessagesToChat(chatRepository, lastMessageHolder)
            }
            // TODO do we need to intercept failed nodes too?
        }

        private suspend fun AIAgentContext.writeOldChatMessagesToPrompt(
            chatRepository: ChatRepository,
            lastMessageHolder: AtomicReference<Message?>,
        ) {
            val chat = chatRepository.requireChat(runId)
            llm.writeSession {
                for (event in chat.events().replayCache.filterIsInstance<Event.Message>()) {
                    appendPrompt {
                        message(event.message)
                    }
                    lastMessageHolder.set(event.message)
                }
            }
        }

        private suspend fun AIAgentGraphContextBase.writeNewPromptMessagesToChat(
            chatRepository: ChatRepository,
            lastMessageHolder: AtomicReference<Message?>,
        ) {
            val lastMessage = lastMessageHolder.get()
            val messagesSinceLast =
                llm.prompt.messages
                    .takeLastWhile { it != lastMessage }
            if (messagesSinceLast.isNotEmpty()) {
                val chat = chatRepository.requireChat(runId)
                for (message in messagesSinceLast) {
                    val event = Event.Message(Uuid.random(), message)
                    chat.append(event)
                    lastMessageHolder.set(message)
                }
            }
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
