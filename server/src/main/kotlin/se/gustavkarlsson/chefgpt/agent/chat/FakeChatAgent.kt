package se.gustavkarlsson.chefgpt.agent.chat

import ai.koog.prompt.message.ResponseMetaInfo
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.EventRepository
import kotlin.time.Clock
import ai.koog.prompt.message.Message as KoogMessage

class FakeChatAgent(
    private val eventRepository: EventRepository,
    private val clock: Clock = Clock.System,
) : ChatAgent {
    override suspend fun run(
        userId: UserId,
        chatId: ChatId,
    ) {
        val message =
            KoogMessage.Assistant(
                content = "This is a fake response from the dummy agent.",
                metaInfo = ResponseMetaInfo(clock.now()),
            )
        eventRepository.append(chatId, Event.Message(EventId.random(), message, attachments = emptyList()))
    }
}
