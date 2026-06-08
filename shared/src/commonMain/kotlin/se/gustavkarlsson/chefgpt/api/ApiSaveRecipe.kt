package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Identifies a recipe to save by its Spoonacular id. The server looks the
// recipe up before storing it.
@Serializable
@SerialName("api-save-recipe")
data class ApiSaveRecipe(
    val spoonacularId: Int,
)
