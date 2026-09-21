package se.gustavkarlsson.chefgpt.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.EventRepository
import kotlin.time.Clock

/**
 * Lets the agent name the current chat. Scoped to a single [userId] and [chatId], so the agent
 * can only ever name the chat it is currently running in.
 */
class RenameChatTool(
    private val chatRepository: ChatRepository,
    private val eventRepository: EventRepository,
    private val userId: UserId,
    private val chatId: ChatId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Set a short, descriptive name for the current chat based on what the user wants to do. " +
            "Call this once you understand the conversations topic or the user's goal. " +
            "Call it again when that topic changes. " +
            "Returns true if the chat was named, false if it could not be found.",
    )
    suspend fun renameChat(
        @LLMDescription("A short, descriptive name for the chat, e.g. 'Quick weeknight pasta'.")
        name: String,
    ): Boolean {
        val renamed = chatRepository.rename(userId, chatId, name)
        if (renamed) {
            eventRepository.append(chatId, Event.ChatNamed(EventId.random(), Clock.System.now(), name))
        }
        return renamed
    }
}
