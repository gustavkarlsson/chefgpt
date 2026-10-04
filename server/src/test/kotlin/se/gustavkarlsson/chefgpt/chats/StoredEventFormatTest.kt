package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.common.EventId
import se.gustavkarlsson.chefgpt.api.common.JoinId
import se.gustavkarlsson.chefgpt.chefGptJson
import se.gustavkarlsson.chefgpt.files.UploadedFile
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

// Pins the serialized form of events as stored in the event.json column. The strings below are
// the storage format's contract: changing any of them is a data migration, not a refactor.
class StoredEventFormatTest {
    private val json = chefGptJson(strict = false)
    private val timestamp = Instant.parse("2026-01-02T03:04:05Z")

    @Test
    fun `serializes a user message`() {
        val event =
            Event.Message(
                id = EventId.parseOrNull("b02cc4c2-66c0-4e60-9385-27cd9e08dcd2")!!,
                message =
                    ChatMessage.User(
                        id = null,
                        timestamp = timestamp,
                        metadata = null,
                        parts =
                            listOf(
                                TextPart("Hello", cacheControl = null),
                                AttachmentPart(
                                    AttachmentSource.Image(
                                        AttachmentContent.Url("https://example.com/pic.jpg"),
                                        "jpg",
                                        "image/jpeg",
                                        "pic.jpg",
                                    ),
                                    cacheControl = null,
                                ),
                            ),
                    ),
                attachments = listOf(UploadedFile("https://example.com/pic.jpg", "image/jpeg", "pic.jpg")),
            )

        assertEquals(
            """{"type":"message","id":"b02cc4c2-66c0-4e60-9385-27cd9e08dcd2",""" +
                """"message":{"type":"user-message",""" +
                """"parts":[{"type":"text","text":"Hello"},""" +
                """{"type":"attachment","source":{"type":"image","content":""" +
                """{"type":"url","url":"https://example.com/pic.jpg"},""" +
                """"format":"jpg","mimeType":"image/jpeg","fileName":"pic.jpg"}}],""" +
                """"metaInfo":{"timestamp":"2026-01-02T03:04:05Z"}},""" +
                """"attachments":[{"url":"https://example.com/pic.jpg","mimeType":"image/jpeg","fileName":"pic.jpg"}]}""",
            json.encodeToString(event.toStored(json)),
        )
    }

    @Test
    fun `serializes an assistant message with reasoning and a tool call`() {
        val event =
            Event.Message(
                id = EventId.parseOrNull("39a9ca52-ce44-4803-b107-9f81e3cf4a7e")!!,
                message =
                    ChatMessage.Assistant(
                        id = null,
                        timestamp = timestamp,
                        metadata = null,
                        parts =
                            listOf(
                                ReasoningPart(
                                    content = listOf("thinking hard"),
                                    summary = listOf("thinking"),
                                    encrypted = null,
                                    id = null,
                                    cacheControl = null,
                                ),
                                TextPart("Here you go", cacheControl = null),
                                ToolCallPart(
                                    id = "call-1",
                                    tool = "getRecipe",
                                    args = """{"recipeId":"abc"}""",
                                    cacheControl = null,
                                ),
                            ),
                        finishReason = "tool_calls",
                        rawResponse = """{"some":"provider-data"}""",
                        totalTokensCount = 100,
                        inputTokensCount = 40,
                        outputTokensCount = 60,
                        modelId = "test-model",
                    ),
                attachments = emptyList(),
            )

        assertEquals(
            """{"type":"message","id":"39a9ca52-ce44-4803-b107-9f81e3cf4a7e",""" +
                """"message":{"type":"assistant-message",""" +
                """"parts":[{"type":"reasoning","content":["thinking hard"],"summary":["thinking"]},""" +
                """{"type":"text","text":"Here you go"},""" +
                """{"type":"tool-call","id":"call-1","tool":"getRecipe",""" +
                """"args":"{\"recipeId\":\"abc\"}"}],""" +
                """"metaInfo":{"timestamp":"2026-01-02T03:04:05Z","totalTokensCount":100,""" +
                """"inputTokensCount":40,"outputTokensCount":60,"modelId":"test-model"},""" +
                """"finishReason":"tool_calls","rawResponse":{"some":"provider-data"}},""" +
                """"attachments":[]}""",
            json.encodeToString(event.toStored(json)),
        )
    }

    @Test
    fun `round-trips a tool result message`() {
        val event =
            Event.Message(
                id = EventId.random(),
                message =
                    ChatMessage.User(
                        id = null,
                        timestamp = timestamp,
                        metadata = null,
                        parts =
                            listOf(
                                ToolResultPart(
                                    id = "result-1",
                                    tool = "getRecipe",
                                    parts = listOf(TextPart("""{"title":"Soup"}""", cacheControl = null)),
                                    isError = false,
                                    cacheControl = null,
                                ),
                            ),
                    ),
                attachments = emptyList(),
            )

        assertEquals(
            event,
            json.decodeFromString<StoredEvent>(json.encodeToString(event.toStored(json))).toDomain(json),
        )
    }

    @Test
    fun `round-trips a system message`() {
        val event =
            Event.Message(
                id = EventId.random(),
                message =
                    ChatMessage.System(
                        id = null,
                        timestamp = timestamp,
                        metadata = null,
                        parts = listOf(TextPart("You are a chef", cacheControl = null)),
                    ),
                attachments = emptyList(),
            )

        assertEquals(
            event,
            json.decodeFromString<StoredEvent>(json.encodeToString(event.toStored(json))).toDomain(json),
        )
    }

    @Test
    fun `round-trips user-joined and chat-named events`() {
        val userJoined = Event.UserJoined(EventId.random(), timestamp, JoinId.random())
        val chatNamed = Event.ChatNamed(EventId.random(), timestamp, "Dinner ideas")

        assertEquals(
            userJoined,
            json.decodeFromString<StoredEvent>(json.encodeToString(userJoined.toStored(json))).toDomain(json),
        )
        assertEquals(
            chatNamed,
            json.decodeFromString<StoredEvent>(json.encodeToString(chatNamed.toStored(json))).toDomain(json),
        )
    }

    @Test
    fun `round-trips binary attachment content as base64`() {
        val bytes = byteArrayOf(1, 2, 3)
        val event =
            Event.Message(
                id = EventId.random(),
                message =
                    ChatMessage.User(
                        id = null,
                        timestamp = timestamp,
                        metadata = null,
                        parts =
                            listOf(
                                AttachmentPart(
                                    AttachmentSource.File(
                                        AttachmentContent.BinaryBytes(bytes),
                                        "bin",
                                        "application/octet-stream",
                                        "data.bin",
                                    ),
                                    cacheControl = null,
                                ),
                            ),
                    ),
                attachments = emptyList(),
            )

        val decoded = json.decodeFromString<StoredEvent>(json.encodeToString(event.toStored(json))).toDomain(json)

        val decodedPart =
            assertIs<AttachmentPart>(
                (decoded as Event.Message).message.let { (it as ChatMessage.User).parts.single() },
            )
        val decodedContent = assertIs<AttachmentContent.BinaryBytes>(decodedPart.source.content)
        assertContentEquals(bytes, decodedContent.data)
    }
}
