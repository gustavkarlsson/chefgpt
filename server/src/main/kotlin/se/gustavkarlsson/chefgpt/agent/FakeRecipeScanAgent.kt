package se.gustavkarlsson.chefgpt.agent

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.toSummary
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.recipes.NewRecipe
import se.gustavkarlsson.chefgpt.recipes.RecipeStore

class FakeRecipeScanAgent(
    private val recipeStore: RecipeStore,
) : RecipeScanAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<ApiAttachment>,
    ): Result<List<ApiRecipeSummary>, String> {
        val recipe =
            recipeStore.saveRecipe(
                userId,
                NewRecipe(
                    title = "Pasta al pomodoro",
                    steps = listOf("Cook the pasta.", "Stir in the sauce."),
                    imageUrl = null,
                    description = "A simple tomato pasta.",
                    preparationDuration = null,
                    cookingDuration = null,
                    duration = null,
                    servings = null,
                    ingredients = emptyList(),
                    nutrients = emptyList(),
                    spoonacularId = null,
                ),
            )
        return Ok(listOf(recipe.toSummary()))
    }
}
