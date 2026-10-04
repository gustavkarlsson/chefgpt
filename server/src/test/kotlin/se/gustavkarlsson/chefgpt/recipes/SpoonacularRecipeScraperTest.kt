package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.chefGptJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SpoonacularRecipeScraperTest {
    @Test
    fun `returns the recipe when the client succeeds`() =
        runTest {
            val scraper = SpoonacularRecipeScraper(Spoonacular(FakeRecipeClient(), chefGptJson(strict = false)))

            val recipe = assertNotNull(scraper.scrape("https://example.com/recipe", null))

            assertEquals("Extracted Recipe", recipe.title)
        }

    @Test
    fun `returns the partial recipe when the steps cannot be parsed`() =
        runTest {
            val scraper = SpoonacularRecipeScraper(Spoonacular(NoStepsClient(), chefGptJson(strict = false)))

            val recipe = assertNotNull(scraper.scrape("https://example.com/recipe", null))

            assertEquals("Extracted Recipe", recipe.title)
            assertEquals(emptyList(), recipe.steps)
        }

    @Test
    fun `returns null when the page has no recipe`() =
        runTest {
            val scraper = SpoonacularRecipeScraper(Spoonacular(NoInstructionsClient(), chefGptJson(strict = false)))

            val recipe = scraper.scrape("https://example.com/not-a-recipe", null)

            assertNull(recipe)
        }

    @Test
    fun `throws when the client fails`() =
        runTest {
            val scraper = SpoonacularRecipeScraper(Spoonacular(ThrowingExtractClient(), chefGptJson(strict = false)))

            assertFailsWith<IllegalStateException> {
                scraper.scrape("https://example.com/recipe", null)
            }
        }
}
