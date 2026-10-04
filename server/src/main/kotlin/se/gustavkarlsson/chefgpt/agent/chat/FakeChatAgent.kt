package se.gustavkarlsson.chefgpt.agent.chat

import se.gustavkarlsson.chefgpt.api.common.ChatId
import se.gustavkarlsson.chefgpt.api.common.EventId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chats.ChatMessage
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.TextPart
import kotlin.time.Clock

class FakeChatAgent(
    private val eventRepository: EventRepository,
    private val clock: Clock = Clock.System,
) : ChatAgent {
    override suspend fun run(
        userId: UserId,
        chatId: ChatId,
    ) {
        val message =
            ChatMessage.Assistant(
                id = null,
                timestamp = clock.now(),
                metadata = null,
                parts = listOf(TextPart("This is a fake response from the dummy agent.", cacheControl = null)),
                finishReason = null,
                rawResponse = null,
                totalTokensCount = null,
                inputTokensCount = null,
                outputTokensCount = null,
                modelId = null,
            )
        eventRepository.append(chatId, Event.Message(EventId.random(), message, attachments = emptyList()))
    }
}
