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
    class Config : FeatureConfig() {
        var chatRepository: ChatRepository? = null
    }

    companion object Feature :
        AIAgentGraphFeature<Config, EventBackedChatMemory> {
        override val key: AIAgentStorageKey<EventBackedChatMemory> =
            AIAgentStorageKey("agents-features-event-backed-chat-memory")

        override fun createInitialConfig(agentConfig: AIAgentConfig): Config = Config()

        override fun install(
            config: Config,
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
            val lastSyncedMessageHolder = AtomicReference<Message?>(null)

            pipeline.interceptStrategyStarting(this) {
                it.context.writeAllChatMessagesToPrompt(chatRepository, lastSyncedMessageHolder)
            }

            pipeline.interceptNodeExecutionCompleted(this) {
                it.context.writeNewPromptMessagesToChat(chatRepository, lastSyncedMessageHolder)
            }
            // TODO Test if interceptNodeExecutionCompleted still gets executed if execution fails. If so, remove interceptNodeExecutionFailed
            pipeline.interceptNodeExecutionFailed(this) {
                it.context.writeNewPromptMessagesToChat(chatRepository, lastSyncedMessageHolder)
            }
        }

        private suspend fun AIAgentContext.writeAllChatMessagesToPrompt(
            chatRepository: ChatRepository,
            lastSyncedMessageHolder: AtomicReference<Message?>,
        ) {
            val chat = chatRepository.requireChat(runId)
            llm.writeSession {
                val chatMessages =
                    chat
                        .events()
                        .replayCache
                        .filterIsInstance<Event.Message>()
                        .map { it.message }
                for (message in chatMessages) {
                    appendPrompt {
                        message(message)
                    }
                }
                chatMessages.lastOrNull()?.let { message ->
                    lastSyncedMessageHolder.set(message)
                }
            }
        }

        private suspend fun AIAgentGraphContextBase.writeNewPromptMessagesToChat(
            chatRepository: ChatRepository,
            lastSyncedMessageHolder: AtomicReference<Message?>,
        ) {
            val lastSyncedMessage = lastSyncedMessageHolder.get()
            val newPromptMessages =
                llm.prompt.messages
                    .takeLastWhile { it != lastSyncedMessage }

            if (newPromptMessages.isNotEmpty()) {
                val chat = chatRepository.requireChat(runId)
                for (message in newPromptMessages) {
                    val event = Event.Message(Uuid.random(), message)
                    chat.append(event)
                }
                lastSyncedMessageHolder.set(newPromptMessages.last())
            }
        }
    }
}

private suspend fun ChatRepository.requireChat(runId: String): Chat {
    val chatId = ChatId.parse(runId)
    val chat = this[chatId] ?: throw NoSuchElementException("No chat with ID $chatId found")
    return chat
}
