package se.gustavkarlsson.chefgpt.api.recipes.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.common.ImageUrl
import se.gustavkarlsson.chefgpt.api.common.RecipeId
import se.gustavkarlsson.chefgpt.api.common.SpoonacularId

@Serializable
@SerialName("api-recipe-summary")
data class ApiRecipeSummary(
    val id: RecipeId,
    val title: String,
    val spoonacularId: SpoonacularId?,
    val imageUrl: ImageUrl?,
    val favorite: Boolean,
    val modifiedFrom: RecipeId?,
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
