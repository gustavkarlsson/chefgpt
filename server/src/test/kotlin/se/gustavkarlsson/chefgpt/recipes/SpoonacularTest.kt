package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.chefGptJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class SpoonacularTest {
    private val spoonacular = Spoonacular(FakeRecipeClient(), chefGptJson(strict = false))

    @Test
    fun `scrapes a recipe from a url`() =
        runTest {
            val recipe = assertNotNull(spoonacular.scrape("https://example.com/recipe"))

            assertEquals("Extracted Recipe", recipe.title)
            assertEquals(
                listOf("Cook pasta according to package directions.", "Drain and serve."),
                recipe.steps,
            )
            assertEquals(
                listOf(
                    RecipeIngredient("chicken breast", "500", "g"),
                    RecipeIngredient("olive oil", "2", "tbsp"),
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
            val spoonacular = Spoonacular(NoInstructionsClient(), chefGptJson(strict = false))

            val recipe = spoonacular.scrape("https://example.com/not-a-recipe")

            assertNull(recipe)
        }

    @Test
    fun `returns a partial recipe when the instructions cannot be parsed`() =
        runTest {
            val spoonacular = Spoonacular(NoStepsClient(), chefGptJson(strict = false))

            val recipe = assertNotNull(spoonacular.scrape("https://example.com/recipe"))

            assertEquals("Extracted Recipe", recipe.title)
            assertEquals(emptyList(), recipe.steps)
        }

    @Test
    fun `throws when the extract call fails`() =
        runTest {
            val spoonacular = Spoonacular(ThrowingExtractClient(), chefGptJson(strict = false))

            assertFailsWith<IllegalStateException> {
                spoonacular.scrape("https://example.com/recipe")
            }
        }
}
