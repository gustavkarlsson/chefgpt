package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Identifies the Spoonacular recipe to save as a recipe summary.
@Serializable
@SerialName("api-save-spoonacular-recipe")
data class ApiSaveSpoonacularRecipe(
    val spoonacularId: SpoonacularId,
)
