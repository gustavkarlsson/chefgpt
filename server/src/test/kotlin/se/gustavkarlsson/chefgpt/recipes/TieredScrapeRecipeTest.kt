package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.agent.scraperecipe.FakeScrapeRecipeAgent
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.chefGptJson
import se.gustavkarlsson.chefgpt.files.FakeHtmlLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val URL = "https://example.com/recipe"

class TieredScrapeRecipeTest {
    @Test
    fun `returns the first complete recipe and stops`() =
        runTest {
            val first = FakeScraper(partialRecipe(), error = null)
            val second = FakeScraper(completeRecipe(), error = null)
            val third = FakeScraper(completeRecipe(), error = null)
            val tiered = TieredScrapeRecipe(listOf(first, second, third))

            val recipe = assertNotNull(tiered.scrape(URL, null))

            assertEquals("Complete recipe", recipe.title)
            assertEquals(1, first.invocations)
            assertEquals(1, second.invocations)
            assertEquals(0, third.invocations)
        }

    @Test
    fun `passes the previous result to the next scraper as hints`() =
        runTest {
            val first = FakeScraper(partialRecipe(), error = null)
            val second = FakeScraper(completeRecipe(), error = null)
            val tiered = TieredScrapeRecipe(listOf(first, second))

            tiered.scrape(URL, null)

            assertEquals("Partial recipe", second.receivedPartialRecipe?.title)
        }

    @Test
    fun `skips a scraper that finds nothing`() =
        runTest {
            val first = FakeScraper(null, error = null)
            val second = FakeScraper(completeRecipe(), error = null)
            val tiered = TieredScrapeRecipe(listOf(first, second))

            val recipe = assertNotNull(tiered.scrape(URL, null))

            assertEquals("Complete recipe", recipe.title)
        }

    @Test
    fun `returns null when no scraper finds a complete recipe`() =
        runTest {
            val tiered =
                TieredScrapeRecipe(
                    listOf(
                        FakeScraper(partialRecipe(), error = null),
                        FakeScraper(null, error = null),
                    ),
                )

            val recipe = tiered.scrape(URL, null)

            assertNull(recipe)
        }

    @Test
    fun `continues past a failing scraper`() =
        runTest {
            val first = FakeScraper(null, error = IllegalStateException("extract failed"))
            val second = FakeScraper(completeRecipe(), error = null)
            val tiered = TieredScrapeRecipe(listOf(first, second))

            val recipe = assertNotNull(tiered.scrape(URL, null))

            assertEquals("Complete recipe", recipe.title)
        }

    @Test
    fun `throws the first scraper's failure when nothing completes`() =
        runTest {
            val tiered =
                TieredScrapeRecipe(
                    listOf(
                        FakeScraper(null, error = IllegalStateException("extract failed")),
                        FakeScraper(null, error = null),
                    ),
                )

            assertFailsWith<IllegalStateException> {
                tiered.scrape(URL, null)
            }
        }

    @Test
    fun `returns a complete partial recipe without scraping`() =
        runTest {
            val first = FakeScraper(completeRecipe(), error = null)
            val tiered = TieredScrapeRecipe(listOf(first))

            val recipe = assertNotNull(tiered.scrape(URL, completeRecipe()))

            assertEquals("Complete recipe", recipe.title)
            assertEquals(0, first.invocations)
        }

    @Test
    fun `scrapes through the real chain when the first scraper finds no recipe`() =
        runTest {
            val chain =
                TieredScrapeRecipe(
                    listOf(
                        SpoonacularScrapeRecipe(Spoonacular(NoInstructionsClient(), chefGptJson(strict = false))),
                        JsonLdScrapeRecipe(FakeHtmlLoader(), RecipeJsonLdParser(chefGptJson(strict = false))),
                        FakeScrapeRecipeAgent(),
                    ),
                )

            val recipe = assertNotNull(chain.scrape("https://example.com/not-a-recipe", null))

            assertEquals("Fake scraped recipe", recipe.title)
        }
}

private class FakeScraper(
    private val result: NewRecipe?,
    private val error: Exception?,
) : RecipeScraper {
    var invocations: Int = 0
        private set
    var receivedPartialRecipe: NewRecipe? = null
        private set

    override suspend fun scrape(
        url: String,
        partialRecipe: NewRecipe?,
    ): NewRecipe? {
        invocations++
        receivedPartialRecipe = partialRecipe
        error?.let { throw it }
        return result
    }
}

private fun completeRecipe() =
    NewRecipe(
        title = "Complete recipe",
        steps = listOf("A step."),
        imageUrl = null,
        description = null,
        preparationDuration = null,
        cookingDuration = null,
        duration = null,
        servings = null,
        ingredients = listOf(ApiRecipeIngredient("flour", "2", "cups")),
        nutrients = emptyList(),
        spoonacularId = null,
    )

private fun partialRecipe() =
    NewRecipe(
        title = "Partial recipe",
        steps = emptyList(),
        imageUrl = null,
        description = null,
        preparationDuration = null,
        cookingDuration = null,
        duration = null,
        servings = null,
        ingredients = listOf(ApiRecipeIngredient("flour", "2", "cups")),
        nutrients = emptyList(),
        spoonacularId = null,
    )
