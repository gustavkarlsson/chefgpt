package se.gustavkarlsson.chefgpt.chats

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.auth.UserId
import kotlin.time.Clock

/**
 * Tools the agent can use to name the current chat. Scoped to a single [userId] and
 * [chatId], so the agent can only ever name the chat it is currently running in.
 */
@Suppress("unused")
class ChatNamingTools(
    private val chatRepository: ChatRepository,
    private val eventRepository: EventRepository,
    private val userId: UserId,
    private val chatId: ChatId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Set a short, descriptive name for the current chat based on what the user wants to do. " +
            "Call this once you understand the user's goal. Calling it again renames the chat. " +
            "Returns true if the chat was named, false if it could not be found.",
    )
    suspend fun nameChat(
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
