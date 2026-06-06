package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IngredientEmojiResolverTest {
    private val resolver = runBlocking { IngredientEmojiResolver.Factory().create() }

    @Test
    fun `resolves a bare head noun of a qualified emoji`() {
        // Emoji.kt names the apple emoji "red-apple", so a plain "apple" must fall back to its head noun.
        assertEquals("🍎", resolver.resolve("apple")?.details?.string)
    }

    @Test
    fun `singularizes plurals`() {
        assertEquals("🍎", resolver.resolve("apples")?.details?.string)
    }

    @Test
    fun `prefers an exact alias over a head-noun match`() {
        assertEquals("🍏", resolver.resolve("green apple")?.details?.string)
    }

    @Test
    fun `resolves a simple single-word alias`() {
        assertEquals("🍌", resolver.resolve("banana")?.details?.string)
    }

    @Test
    fun `returns null for an unknown ingredient`() {
        assertNull(resolver.resolve("gravel and dust"))
    }

    @Test
    fun `treats every emoji as unsupported when the validator rejects all`() {
        val emptyResolver = runBlocking { IngredientEmojiResolver.Factory(isSupported = { false }).create() }
        assertNull(emptyResolver.resolve("apple"))
    }
}
