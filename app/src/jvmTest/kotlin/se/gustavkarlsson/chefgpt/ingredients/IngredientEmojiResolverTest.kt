package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IngredientEmojiResolverTest {
    private val resolver = IngredientEmojiResolver()

    @Test
    fun `resolves a bare head noun of a qualified emoji`() =
        runBlocking {
            // Emoji.kt names the apple emoji "red-apple", so a plain "apple" must fall back to its head noun.
            assertEquals("🍎", resolver.resolve("apple")?.details?.string)
        }

    @Test
    fun `singularizes plurals`() =
        runBlocking {
            assertEquals("🍎", resolver.resolve("apples")?.details?.string)
        }

    @Test
    fun `prefers an exact alias over a head-noun match`() =
        runBlocking {
            assertEquals("🍏", resolver.resolve("green apple")?.details?.string)
        }

    @Test
    fun `resolves a simple single-word alias`() =
        runBlocking {
            assertEquals("🍌", resolver.resolve("banana")?.details?.string)
        }

    @Test
    fun `returns null for an unknown ingredient`() =
        runBlocking {
            assertNull(resolver.resolve("gravel and dust"))
        }

    @Test
    fun `treats every emoji as unsupported when the validator rejects all`() =
        runBlocking {
            assertNull(IngredientEmojiResolver(isSupported = { false }).resolve("apple"))
        }
}
