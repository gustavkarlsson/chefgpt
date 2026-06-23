package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.ApiAgentMessageChunk
import se.gustavkarlsson.chefgpt.api.ApiAgentMessageChunk.MultipleChoiceQuestion
import se.gustavkarlsson.chefgpt.api.ApiAgentMessageChunk.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseAgentMessageChunksTest {
    @Test
    fun `returns single text chunk for plain text`() {
        val text = "Hello! What are we **cooking** today?"

        val result = parseAgentMessageChunks(text)

        assertEquals(listOf(Text(text)), result)
    }

    @Test
    fun `returns empty list for blank text`() {
        val result = parseAgentMessageChunks("  \n\n ")

        assertEquals(emptyList(), result)
    }

    @Test
    fun `extracts question between text chunks`() {
        val text =
            """
            Let's pick a dish!

            ```multiple-choice-question
            {
                "question": "What would you like to cook?",
                "answers": [
                    "Pasta",
                    "Pancakes"
                ]
            }
            ```

            Take your time.
            """.trimIndent()

        val result = parseAgentMessageChunks(text)

        assertEquals(
            listOf(
                Text("Let's pick a dish!"),
                MultipleChoiceQuestion("What would you like to cook?", listOf("Pasta", "Pancakes")),
                Text("Take your time."),
            ),
            result,
        )
    }

    @Test
    fun `extracts question when message contains only the code block`() {
        val text =
            """
            ```multiple-choice-question
            {"question": "Spicy or mild?", "answers": ["Spicy", "Mild"]}
            ```
            """.trimIndent()

        val result = parseAgentMessageChunks(text)

        assertEquals(listOf(MultipleChoiceQuestion("Spicy or mild?", listOf("Spicy", "Mild"))), result)
    }

    @Test
    fun `trims question and answers`() {
        val text =
            """
            ```multiple-choice-question
            {"question": " Spicy or mild? ", "answers": [" Spicy ", " Mild "]}
            ```
            """.trimIndent()

        val result = parseAgentMessageChunks(text)

        assertEquals(listOf(MultipleChoiceQuestion("Spicy or mild?", listOf("Spicy", "Mild"))), result)
    }

    @Test
    fun `keeps block with invalid json as text`() {
        val text =
            """
            ```multiple-choice-question
            not json
            ```
            """.trimIndent()

        val result = parseAgentMessageChunks(text)

        assertEquals(listOf(Text(text)), result)
    }

    @Test
    fun `keeps block with a single answer as text`() {
        val text =
            """
            ```multiple-choice-question
            {"question": "Spicy or mild?", "answers": ["Spicy"]}
            ```
            """.trimIndent()

        val result = parseAgentMessageChunks(text)

        assertEquals(listOf(Text(text)), result)
    }

    @Test
    fun `keeps block with blank question as text`() {
        val text =
            """
            ```multiple-choice-question
            {"question": " ", "answers": ["Spicy", "Mild"]}
            ```
            """.trimIndent()

        val result = parseAgentMessageChunks(text)

        assertEquals(listOf(Text(text)), result)
    }

    @Test
    fun `keeps unclosed block as text`() {
        val text =
            """
            ```multiple-choice-question
            {"question": "Spicy or mild?", "answers": ["Spicy", "Mild"]}
            """.trimIndent()

        val result = parseAgentMessageChunks(text)

        assertEquals(listOf(Text(text)), result)
    }

    @Test
    fun `keeps code block of other type as text`() {
        val text =
            """
            ```json
            {"question": "Spicy or mild?", "answers": ["Spicy", "Mild"]}
            ```
            """.trimIndent()

        val result = parseAgentMessageChunks(text)

        assertEquals(listOf(Text(text)), result)
    }

    @Test
    fun `extracts multiple questions in one message`() {
        val text =
            """
            ```multiple-choice-question
            {"question": "Spicy or mild?", "answers": ["Spicy", "Mild"]}
            ```
            ```multiple-choice-question
            {"question": "Sweet or savory?", "answers": ["Sweet", "Savory"]}
            ```
            """.trimIndent()

        val result = parseAgentMessageChunks(text)

        assertEquals(
            listOf(
                MultipleChoiceQuestion("Spicy or mild?", listOf("Spicy", "Mild")),
                MultipleChoiceQuestion("Sweet or savory?", listOf("Sweet", "Savory")),
            ),
            result,
        )
    }

    @Test
    fun `ignores unknown json keys`() {
        val text =
            """
            ```multiple-choice-question
            {"question": "Spicy or mild?", "answers": ["Spicy", "Mild"], "extra": true}
            ```
            """.trimIndent()

        val result = parseAgentMessageChunks(text)

        assertEquals(listOf(MultipleChoiceQuestion("Spicy or mild?", listOf("Spicy", "Mild"))), result)
    }
}
