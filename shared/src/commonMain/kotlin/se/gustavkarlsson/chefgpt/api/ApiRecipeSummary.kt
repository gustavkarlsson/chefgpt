package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-recipe-summary")
data class ApiRecipeSummary(
    val id: RecipeId,
    val title: String,
    val spoonacularId: SpoonacularId? = null,
    val imageUrl: ImageUrl? = null,
    val favorite: Boolean = false,
    val modifiedFrom: RecipeId? = null,
)

fun ApiRecipe.toSummary(): ApiRecipeSummary =
    ApiRecipeSummary(
        id = id,
        title = title,
        spoonacularId = spoonacularId,
        imageUrl = imageUrl,
        favorite = favorite,
        modifiedFrom = modifiedFrom,
    )
