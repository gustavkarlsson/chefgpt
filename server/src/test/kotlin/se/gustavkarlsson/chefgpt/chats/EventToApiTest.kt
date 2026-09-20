package se.gustavkarlsson.chefgpt.chats

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import se.gustavkarlsson.chefgpt.api.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.ApiAgentMessageChunk.MultipleChoiceQuestion
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.toApiOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class EventToApiTest {
    @Test
    fun `parses a multiple choice question through the event mapping`() {
        val message =
            Message.Assistant(
                content =
                    """
                    ```multiple-choice-question
                    {"question": "Spicy or mild?", "answers": ["Spicy", "Mild"]}
                    ```
                    """.trimIndent(),
                metaInfo = ResponseMetaInfo(Clock.System.now()),
            )
        val event = Event.Message(EventId.random(), message, attachments = emptyList())

        val apiEvent = event.toApiOrNull()

        assertEquals(
            ApiAgentMessage(
                id = event.id,
                timestamp = event.timestamp,
                chunks = listOf(MultipleChoiceQuestion("Spicy or mild?", listOf("Spicy", "Mild"))),
            ),
            apiEvent,
        )
    }
}
