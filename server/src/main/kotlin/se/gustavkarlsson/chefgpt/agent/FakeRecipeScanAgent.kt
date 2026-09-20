package se.gustavkarlsson.chefgpt.agent

import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.recipes.NewRecipe
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository

class FakeRecipeScanAgent(
    private val recipeRepository: RecipeRepository,
) : RecipeScanAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<String> {
        val recipe =
            recipeRepository.saveRecipe(
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
        return listOf(recipe.title)
    }
}
