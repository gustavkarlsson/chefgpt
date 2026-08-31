package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeId

data class RecipeSummary(
    val id: RecipeId,
    val title: String,
    val imageUrl: ImageUrl?,
    val favorite: Boolean,
    val modifiedFrom: RecipeId?,
)
