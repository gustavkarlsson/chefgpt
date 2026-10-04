package se.gustavkarlsson.chefgpt.api.recipes.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.common.SpoonacularId

// Identifies the Spoonacular recipe to save as a recipe summary.
@Serializable
@SerialName("api-save-spoonacular-recipe")
data class ApiSaveSpoonacularRecipe(
    val spoonacularId: SpoonacularId,
)
