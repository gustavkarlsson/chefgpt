package se.gustavkarlsson.chefgpt.agent

import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.ResponseMetaInfo
import io.ktor.server.routing.RoutingContext
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.EventRepository
import kotlin.time.Clock
import ai.koog.prompt.message.Message as KoogMessage

class FakeAiAgent(
    private val eventRepository: EventRepository,
    private val clock: Clock = Clock.System,
) : AiAgent {
    override suspend fun RoutingContext.run(chatId: ChatId) {
        val message =
            KoogMessage.Assistant(
                parts = listOf(ContentPart.Text("This is a fake response from the dummy agent.")),
                metaInfo = ResponseMetaInfo(clock.now()),
            )
        eventRepository.append(chatId, Event.Message(EventId.random(), message))
    }
}
