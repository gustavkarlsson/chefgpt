package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-recipe-summary")
data class ApiRecipeSummary(
    val id: RecipeId,
    val title: String,
    val spoonacularId: SpoonacularId,
    val imageUrl: String? = null,
)
