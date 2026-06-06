package se.gustavkarlsson.chefgpt.ingredients

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseEmojiAliasOrNullTest {
    private val database =
        setOf(
            "mushroom",
            "jam",
            "raspberry",
            "apple",
            "mustard",
            "banana",
            "tomato",
            "jalapeno",
            "melon",
            "water-melon",
        )

    @Test
    fun `singularizes simple plural`() {
        val result = parseEmojiAliasOrNull("Mushrooms", database)

        assertEquals("mushroom", result)
    }

    @Test
    fun `singularizes oes plural`() {
        val result = parseEmojiAliasOrNull("tomatoes", database)

        assertEquals("tomato", result)
    }

    @Test
    fun `prefers the head noun of a compound`() {
        val result = parseEmojiAliasOrNull("raspberry jam", database)

        assertEquals("jam", result)
    }

    @Test
    fun `falls back to earlier word when head noun is unknown`() {
        val result = parseEmojiAliasOrNull("raspberry jambalaya", database)

        assertEquals("raspberry", result)
    }

    @Test
    fun `ignores parenthetical qualifiers`() {
        val result = parseEmojiAliasOrNull("apples (pink lady)", database)

        assertEquals("apple", result)
    }

    @Test
    fun `ignores unknown possessive`() {
        val result = parseEmojiAliasOrNull("Johnny's mustard", database)

        assertEquals("mustard", result)
    }

    @Test
    fun `splits on hyphens`() {
        val result = parseEmojiAliasOrNull("banana-split", database)

        assertEquals("banana", result)
    }

    @Test
    fun `folds diacritics to the english alphabet`() {
        val result = parseEmojiAliasOrNull("Jalapeños", database)

        assertEquals("jalapeno", result)
    }

    @Test
    fun `matches a multi-word entry from whitespace-separated input`() {
        val result = parseEmojiAliasOrNull("water melon", database)

        assertEquals("water-melon", result)
    }

    @Test
    fun `matches a multi-word entry from hyphenated input`() {
        val result = parseEmojiAliasOrNull("water-melon", database)

        assertEquals("water-melon", result)
    }

    @Test
    fun `singularizes the head of a multi-word entry`() {
        val result = parseEmojiAliasOrNull("water melons", database)

        assertEquals("water-melon", result)
    }

    @Test
    fun `matches a single word that is also part of a multi-word entry`() {
        val result = parseEmojiAliasOrNull("melon", database)

        assertEquals("melon", result)
    }

    @Test
    fun `returns null when nothing matches`() {
        val result = parseEmojiAliasOrNull("gravel and dust", database)

        assertNull(result)
    }

    @Test
    fun `returns null for blank input`() {
        val result = parseEmojiAliasOrNull("   ", database)

        assertNull(result)
    }
}
