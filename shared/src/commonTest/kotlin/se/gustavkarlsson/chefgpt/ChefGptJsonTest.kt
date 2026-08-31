package se.gustavkarlsson.chefgpt

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.MissingFieldException as MissingField

@Serializable
private data class Nullable(
    val required: String,
    val optional: String?,
)

@Serializable
private data class NonNull(
    val required: String,
    val flag: Boolean,
)

class ChefGptJsonTest {
    // Not strict, unlike other tests: this is the test of the non-strict configuration itself.
    private val json = chefGptJson(strict = false)

    @Test
    fun `leaves a null property out when encoding`() {
        val encoded = json.encodeToString(Nullable("a", null))

        assertEquals("""{"required":"a"}""", encoded)
    }

    @Test
    fun `reads an absent key as null for a nullable property without a default`() {
        val decoded = json.decodeFromString<Nullable>("""{"required":"a"}""")

        assertEquals(Nullable("a", null), decoded)
    }

    @Test
    fun `fails on an absent key for a non-null property without a default`() {
        assertFailsWith<MissingField> {
            json.decodeFromString<NonNull>("""{"required":"a"}""")
        }
    }

    @Test
    fun `ignores an unknown key`() {
        val decoded = json.decodeFromString<NonNull>("""{"required":"a","flag":true,"extra":1}""")

        assertEquals(NonNull("a", true), decoded)
    }

    @Test
    fun `fails on an unknown key when strict`() {
        assertFailsWith<SerializationException> {
            chefGptJson(strict = true).decodeFromString<NonNull>("""{"required":"a","flag":true,"extra":1}""")
        }
    }

    @Test
    fun `still reads an absent key as null when strict`() {
        val decoded = chefGptJson(strict = true).decodeFromString<Nullable>("""{"required":"a"}""")

        assertEquals(Nullable("a", null), decoded)
    }
}
