package se.gustavkarlsson.chefgpt.ingredients

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseEmojiIdentifierOrNullTest {
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
        val result = parseEmojiIdentifierOrNull("Mushrooms", database)

        assertEquals("mushroom", result)
    }

    @Test
    fun `singularizes oes plural`() {
        val result = parseEmojiIdentifierOrNull("tomatoes", database)

        assertEquals("tomato", result)
    }

    @Test
    fun `prefers the head noun of a compound`() {
        val result = parseEmojiIdentifierOrNull("raspberry jam", database)

        assertEquals("jam", result)
    }

    @Test
    fun `falls back to earlier word when head noun is unknown`() {
        val result = parseEmojiIdentifierOrNull("raspberry jambalaya", database)

        assertEquals("raspberry", result)
    }

    @Test
    fun `ignores parenthetical qualifiers`() {
        val result = parseEmojiIdentifierOrNull("apples (pink lady)", database)

        assertEquals("apple", result)
    }

    @Test
    fun `ignores unknown possessive`() {
        val result = parseEmojiIdentifierOrNull("Johnny's mustard", database)

        assertEquals("mustard", result)
    }

    @Test
    fun `splits on hyphens`() {
        val result = parseEmojiIdentifierOrNull("banana-split", database)

        assertEquals("banana", result)
    }

    @Test
    fun `folds diacritics to the english alphabet`() {
        val result = parseEmojiIdentifierOrNull("Jalapeños", database)

        assertEquals("jalapeno", result)
    }

    @Test
    fun `matches a multi-word entry from whitespace-separated input`() {
        val result = parseEmojiIdentifierOrNull("water melon", database)

        assertEquals("water-melon", result)
    }

    @Test
    fun `matches a multi-word entry from hyphenated input`() {
        val result = parseEmojiIdentifierOrNull("water-melon", database)

        assertEquals("water-melon", result)
    }

    @Test
    fun `singularizes the head of a multi-word entry`() {
        val result = parseEmojiIdentifierOrNull("water melons", database)

        assertEquals("water-melon", result)
    }

    @Test
    fun `matches a single word that is also part of a multi-word entry`() {
        val result = parseEmojiIdentifierOrNull("melon", database)

        assertEquals("melon", result)
    }

    @Test
    fun `returns null when nothing matches`() {
        val result = parseEmojiIdentifierOrNull("gravel and dust", database)

        assertNull(result)
    }

    @Test
    fun `returns null for blank input`() {
        val result = parseEmojiIdentifierOrNull("   ", database)

        assertNull(result)
    }
}
