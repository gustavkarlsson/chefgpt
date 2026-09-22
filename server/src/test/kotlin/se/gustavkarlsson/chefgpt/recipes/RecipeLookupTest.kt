package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.chefGptJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class RecipeLookupTest {
    private val lookup = RecipeLookup(FakeRecipeClient(), chefGptJson(strict = false))

    @Test
    fun `scrapes a recipe from a url`() =
        runTest {
            val recipe = assertNotNull(lookup.scrape("https://example.com/recipe"))

            assertEquals("Extracted Recipe", recipe.title)
            assertEquals(
                listOf("Cook pasta according to package directions.", "Drain and serve."),
                recipe.steps,
            )
            assertEquals(
                listOf(
                    ApiRecipeIngredient("chicken breast", "500", "g"),
                    ApiRecipeIngredient("olive oil", "2", "tbsp"),
                ),
                recipe.ingredients,
            )
            assertEquals(45.minutes, recipe.duration)
            assertEquals(4..4, recipe.servings)
            assertEquals(ImageUrl("https://img.spoonacular.com/recipes/716429-556x370.jpg"), recipe.imageUrl)
            assertNull(recipe.spoonacularId)
        }

    @Test
    fun `returns null when the page has no instructions`() =
        runTest {
            val lookup = RecipeLookup(NoInstructionsClient(), chefGptJson(strict = false))

            val recipe = lookup.scrape("https://example.com/not-a-recipe")

            assertNull(recipe)
        }
}

private class NoInstructionsClient : RecipeClient by FakeRecipeClient() {
    override suspend fun extractRecipeFromWebsite(
        url: String,
        forceExtraction: Boolean,
        analyze: Boolean,
        includeNutrition: Boolean,
        includeTaste: Boolean,
    ): String = """{"title":"No recipe here"}"""
}
