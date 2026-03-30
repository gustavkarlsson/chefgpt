package se.gustavkarlsson.chefgpt.setup

import se.gustavkarlsson.chefgpt.recipes.FakeRecipeClient
import se.gustavkarlsson.chefgpt.recipes.RecipeClient
import se.gustavkarlsson.chefgpt.recipes.SpoonacularClient

fun createRecipeClient(
    recipes: String,
    spoonacularApiKey: String?,
): RecipeClient =
    when (recipes) {
        "spoonacular" -> {
            SpoonacularClient(
                requireNotNull(spoonacularApiKey) { "spoonacularApiKey must be set when recipes is 'spoonacular'" },
            )
        }

        "fake" -> {
            FakeRecipeClient()
        }

        else -> {
            error("Unknown recipes option: '$recipes'. Must be 'spoonacular' or 'fake'.")
        }
    }
