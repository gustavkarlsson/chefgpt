package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-recipe")
data class ApiRecipe(
    val id: RecipeId,
    val title: String,
    val url: String,
    val imageUrl: String? = null,
)
