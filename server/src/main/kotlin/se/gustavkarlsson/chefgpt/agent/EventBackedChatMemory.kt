package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.context.AIAgentGraphContextBase
import ai.koog.agents.core.agent.entity.AIAgentStorageKey
import ai.koog.agents.core.feature.AIAgentGraphFeature
import ai.koog.agents.core.feature.config.FeatureConfig
import ai.koog.agents.core.feature.pipeline.AIAgentGraphPipeline
import ai.koog.prompt.message.Message
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.EventRepository
import java.util.concurrent.atomic.AtomicReference

private val logger = LoggerFactory.getLogger(EventRepository::class.java)

class EventBackedChatMemory {
    class Config : FeatureConfig() {
        var eventRepository: EventRepository? = null
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
            val eventRepository =
                requireNotNull(config.eventRepository) {
                    "eventRepository must be set during plugin installation"
                }
            installInternal(eventRepository, pipeline)
            return chatMemory
        }

        private fun installInternal(
            eventRepository: EventRepository,
            pipeline: AIAgentGraphPipeline,
        ) {
            val lastSyncedMessageHolder = AtomicReference<Message?>(null)

            pipeline.interceptStrategyStarting(this) {
                it.context.writeAllChatMessagesToPrompt(eventRepository, lastSyncedMessageHolder)
            }

            pipeline.interceptNodeExecutionCompleted(this) {
                it.context.writeNewPromptMessagesToChat(eventRepository, lastSyncedMessageHolder)
            }
            // TODO Test if interceptNodeExecutionCompleted still gets executed if execution fails. If so, remove interceptNodeExecutionFailed
            pipeline.interceptNodeExecutionFailed(this) {
                it.context.writeNewPromptMessagesToChat(eventRepository, lastSyncedMessageHolder)
            }
        }

        private suspend fun AIAgentContext.writeAllChatMessagesToPrompt(
            eventRepository: EventRepository,
            lastSyncedMessageHolder: AtomicReference<Message?>,
        ) {
            val chatId = ChatId.parse(runId)
            llm.writeSession {
                val chatMessages =
                    eventRepository
                        .getAll(chatId)
                        .filterIsInstance<Event.Message>()
                        .map { it.message }
                val sanitizedMessages = sanitizeMessages(chatMessages)
                if (chatMessages.size != sanitizedMessages.size) {
                    val droppedMessageCount = chatMessages.size - sanitizedMessages.size
                    logger.warn("$droppedMessageCount messages were dropped during sanitization")
                }
                for (message in sanitizedMessages) {
                    appendPrompt {
                        message(message)
                    }
                }
                sanitizedMessages.lastOrNull()?.let { message ->
                    lastSyncedMessageHolder.set(message)
                }
            }
        }

        /**
         * Removes any orphaned [Message.Tool.Call] that is not immediately followed by
         * a [Message.Tool.Result]. This can happen if an agent run was interrupted after
         * saving the tool call but before saving the tool result. Such orphaned calls
         * cause Anthropic to reject the request with a "tool_use without tool_result" error.
         */
        private fun sanitizeMessages(messages: List<Message>): List<Message> =
            buildList {
                for ((index, message) in messages.withIndex()) {
                    if (message is Message.Tool.Call) {
                        val next = messages.getOrNull(index + 1)
                        if (next !is Message.Tool.Result) {
                            // Orphaned tool call — drop it and everything after
                            break
                        }
                    }
                    add(message)
                }
            }

        private suspend fun AIAgentGraphContextBase.writeNewPromptMessagesToChat(
            eventRepository: EventRepository,
            lastSyncedMessageHolder: AtomicReference<Message?>,
        ) {
            val lastSyncedMessage = lastSyncedMessageHolder.get()
            val newPromptMessages =
                llm.prompt.messages
                    .takeLastWhile { it != lastSyncedMessage }

            if (newPromptMessages.isNotEmpty()) {
                val chatId = ChatId.parse(runId)
                for (message in newPromptMessages) {
                    val event = Event.Message(EventId.random(), message)
                    eventRepository.append(chatId, event)
                }
                lastSyncedMessageHolder.set(newPromptMessages.last())
            }
        }
    }
}
