package se.gustavkarlsson.chefgpt.agent.scraperecipe

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.agent.tools.models.toTool
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.recipes.RecipeIngredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

private val INGREDIENTS = listOf(RecipeIngredient("spaghetti", "200", "g"))
private val STEPS = listOf("Cook the spaghetti.")

class ReportRecipeToolTest {
    @Test
    fun `maps reported fields to a recipe`() =
        runTest {
            val tool = ReportRecipeTool()
            tool.reportRecipe(
                title = "Carbonara",
                ingredients = INGREDIENTS.map { it.toTool() },
                steps = STEPS,
                imageUrl = "https://example.com/carbonara.jpg",
                description = "A classic pasta dish.",
                preparationMinutes = 15,
                cookingMinutes = 30,
                totalMinutes = 45,
                servings = 4,
            )

            val recipe = tool.toNewRecipe()

            assertEquals("Carbonara", recipe.title)
            assertEquals(INGREDIENTS, recipe.ingredients)
            assertEquals(STEPS, recipe.steps)
            assertEquals(ImageUrl("https://example.com/carbonara.jpg"), recipe.imageUrl)
            assertEquals("A classic pasta dish.", recipe.description)
            assertEquals(15.minutes, recipe.preparationDuration)
            assertEquals(30.minutes, recipe.cookingDuration)
            assertEquals(45.minutes, recipe.duration)
            assertEquals(4..4, recipe.servings)
        }

    @Test
    fun `treats sentinel defaults as absent`() =
        runTest {
            val tool = ReportRecipeTool()
            tool.reportRecipe(
                title = "Carbonara",
                ingredients = INGREDIENTS.map { it.toTool() },
                steps = STEPS,
            )

            val recipe = tool.toNewRecipe()

            assertNull(recipe.imageUrl)
            assertNull(recipe.description)
            assertNull(recipe.preparationDuration)
            assertNull(recipe.cookingDuration)
            assertNull(recipe.duration)
            assertNull(recipe.servings)
        }
}
