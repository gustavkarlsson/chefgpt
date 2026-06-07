package se.gustavkarlsson.chefgpt.ingredients

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IngredientWordsTest {
    @Test
    fun `matches as a substring anywhere in the name`() {
        val result = IngredientWords.match("err")

        assertTrue(result.contains("blackberries"))
        assertTrue(result.all { it.contains("err") })
    }

    @Test
    fun `is case insensitive`() {
        assertEquals(IngredientWords.match("TOM"), IngredientWords.match("tom"))
        assertTrue(IngredientWords.match("TOM").contains("tomatoes"))
    }

    @Test
    fun `ranks prefix matches before mid-word matches`() {
        val result = IngredientWords.match("pea")

        // "peas", "peaches", "peanuts", "pears" start with the query; "chickpeas" merely contains it.
        assertTrue(result.indexOf("peas") < result.indexOf("chickpeas"))
    }

    @Test
    fun `caps the number of results at the limit`() {
        val result = IngredientWords.match("a", limit = 5)

        assertEquals(5, result.size)
    }

    @Test
    fun `returns nothing for a blank query`() {
        assertTrue(IngredientWords.match("").isEmpty())
        assertTrue(IngredientWords.match("   ").isEmpty())
    }

    @Test
    fun `returns nothing when there is no match`() {
        assertTrue(IngredientWords.match("zzzz").isEmpty())
    }

    @Test
    fun `exposes a sorted catalog`() {
        assertEquals(IngredientWords.all.sorted(), IngredientWords.all)
    }
}
