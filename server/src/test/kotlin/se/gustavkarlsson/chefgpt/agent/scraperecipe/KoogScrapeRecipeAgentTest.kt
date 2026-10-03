package se.gustavkarlsson.chefgpt.agent.scraperecipe

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.recipes.NewRecipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

private const val URL = "https://example.com/recipe"

private val SCRAPED =
    NewRecipe(
        title = "Carbonara",
        steps = listOf("Cook the spaghetti."),
        imageUrl = ImageUrl("https://example.com/carbonara.jpg"),
        description = "A classic pasta dish.",
        preparationDuration = 15.minutes,
        cookingDuration = 30.minutes,
        duration = 45.minutes,
        servings = 4..4,
        ingredients = listOf(ApiRecipeIngredient("spaghetti", "200", "g")),
        nutrients = emptyList(),
        spoonacularId = null,
    )

private val PARTIAL =
    NewRecipe(
        title = "Carbonara",
        steps = emptyList(),
        imageUrl = null,
        description = null,
        preparationDuration = null,
        cookingDuration = null,
        duration = null,
        servings = null,
        ingredients = listOf(ApiRecipeIngredient("spaghetti", "200", "g")),
        nutrients = listOf(ApiNutrient("Calories", "450", "kcal")),
        spoonacularId = null,
    )

class KoogScrapeRecipeAgentTest {
    @Test
    fun `merges the partial recipe's nutrients into the reported recipe`() =
        runTest {
            val agent = KoogScrapeRecipeAgent(FakeScrapeRecipe(SCRAPED))

            val recipe = agent.scrape(URL, PARTIAL)

            assertEquals(SCRAPED.copy(nutrients = PARTIAL.nutrients), recipe)
        }

    @Test
    fun `leaves nutrients empty without a partial recipe`() =
        runTest {
            val agent = KoogScrapeRecipeAgent(FakeScrapeRecipe(SCRAPED))

            val recipe = agent.scrape(URL, null)

            assertEquals(SCRAPED, recipe)
        }

    @Test
    fun `passes the url and partial recipe to the scraper`() =
        runTest {
            val seam = FakeScrapeRecipe(SCRAPED)
            val agent = KoogScrapeRecipeAgent(seam)

            agent.scrape(URL, PARTIAL)

            assertEquals(URL, seam.receivedUrl)
            assertEquals(PARTIAL, seam.receivedPartialRecipe)
        }
}

private class FakeScrapeRecipe(
    private val result: NewRecipe,
) : ScrapeRecipe {
    var receivedUrl: String? = null
        private set
    var receivedPartialRecipe: NewRecipe? = null
        private set

    override suspend fun invoke(
        url: String,
        partialRecipe: NewRecipe?,
    ): NewRecipe {
        receivedUrl = url
        receivedPartialRecipe = partialRecipe
        return result
    }
}
