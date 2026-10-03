package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.agent.convertrecipeunits.ConvertRecipeUnitsAgent
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chefGptJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RecipeScraperTest {
    private val userId = UserId.random()
    private val convertedIngredients = listOf(ApiRecipeIngredient("chicken breast", "1.1", "lb"))

    @Test
    fun `saves the recipe with converted units`() =
        runTest {
            val lookup = RecipeLookup(FakeRecipeClient(), chefGptJson(strict = false))
            val repository = RecipeRepository(InMemoryRecipePersistence())
            val scraper = RecipeScraper(lookup, repository, ConvertingAgent(convertedIngredients))

            val saved = assertNotNull(scraper.scrape(userId, "https://example.com/recipe"))

            assertEquals(convertedIngredients, saved.ingredients)
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
