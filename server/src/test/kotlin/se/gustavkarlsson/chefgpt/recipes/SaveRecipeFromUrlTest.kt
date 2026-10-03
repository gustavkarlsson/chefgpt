package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.agent.convertrecipeunits.ConvertRecipeUnitsAgent
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.FakeFileUploader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SaveRecipeFromUrlTest {
    private val userId = UserId.random()
    private val convertedIngredients = listOf(ApiRecipeIngredient("chicken breast", "1.1", "lb"))

    @Test
    fun `saves the scraped recipe with converted units`() =
        runTest {
            val saver = saver(FixedScraper(scrapedRecipe(), null))

            val saved = assertNotNull(saver.save(userId, "https://example.com/recipe"))

            assertEquals(convertedIngredients, saved.ingredients)
        }

    @Test
    fun `returns null when nothing could be scraped`() =
        runTest {
            val saver = saver(FixedScraper(null, null))

            val saved = saver.save(userId, "https://example.com/recipe")

            assertNull(saved)
        }

    @Test
    fun `keeps an already trusted image url`() =
        runTest {
            val imageUrl = ImageUrl("https://img.spoonacular.com/recipes/716429-556x370.jpg")
            val saver = saver(FixedScraper(scrapedRecipe().copy(imageUrl = imageUrl), null))

            val saved = assertNotNull(saver.save(userId, "https://example.com/recipe"))

            assertEquals(imageUrl, saved.imageUrl)
        }

    @Test
    fun `rethrows the scraper's failure`() =
        runTest {
            val saver = saver(FixedScraper(null, IllegalStateException("extract failed")))

            assertFailsWith<IllegalStateException> {
                saver.save(userId, "https://example.com/recipe")
            }
        }

    private fun saver(scraper: RecipeScraper): SaveRecipeFromUrl =
        SaveRecipeFromUrl(
            scraper = scraper,
            imageRehoster = RecipeImageRehoster(FakeFileUploader()),
            repository = RecipeRepository(InMemoryRecipePersistence()),
            convertRecipeUnits = ConvertingAgent(convertedIngredients),
        )
}

private class FixedScraper(
    private val result: NewRecipe?,
    private val error: Exception?,
) : RecipeScraper {
    override suspend fun scrape(
        url: String,
        partialRecipe: NewRecipe?,
    ): NewRecipe? {
        error?.let { throw it }
        return result
    }
}

private class ConvertingAgent(
    private val ingredients: List<ApiRecipeIngredient>,
) : ConvertRecipeUnitsAgent {
    override suspend fun convert(
        userId: UserId,
        recipe: NewRecipe,
    ): NewRecipe = recipe.copy(ingredients = ingredients)
}

private fun scrapedRecipe() =
    NewRecipe(
        title = "Scraped recipe",
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
