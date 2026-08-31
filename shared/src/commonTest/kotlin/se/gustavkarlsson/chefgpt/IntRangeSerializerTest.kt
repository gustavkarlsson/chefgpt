package se.gustavkarlsson.chefgpt

import kotlin.test.Test
import kotlin.test.assertEquals

class IntRangeSerializerTest {
    private val json = chefGptJson(strict = true)

    @Test
    fun `encodes a range as its bounds`() {
        val encoded = json.encodeToString(IntRangeSerializer, 4..6)

        assertEquals("""{"min":4,"max":6}""", encoded)
    }

    @Test
    fun `encodes a single value range as equal bounds`() {
        val encoded = json.encodeToString(IntRangeSerializer, 4..4)

        assertEquals("""{"min":4,"max":4}""", encoded)
    }

    @Test
    fun `decodes a range from its bounds`() {
        val decoded = json.decodeFromString(IntRangeSerializer, """{"min":4,"max":6}""")

        assertEquals(4..6, decoded)
    }
}
