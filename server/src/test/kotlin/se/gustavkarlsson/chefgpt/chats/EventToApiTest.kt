package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.chats.v1.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiAgentMessageChunk.MultipleChoiceQuestion
import se.gustavkarlsson.chefgpt.api.common.EventId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class EventToApiTest {
    @Test
    fun `parses a multiple choice question through the event mapping`() {
        val message =
            ChatMessage.Assistant(
                id = null,
                timestamp = Clock.System.now(),
                metadata = null,
                parts =
                    listOf(
                        TextPart(
                            """
                            ```multiple-choice-question
                            {"question": "Spicy or mild?", "answers": ["Spicy", "Mild"]}
                            ```
                            """.trimIndent(),
                            cacheControl = null,
                        ),
                    ),
                finishReason = null,
                rawResponse = null,
                totalTokensCount = null,
                inputTokensCount = null,
                outputTokensCount = null,
                modelId = null,
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
