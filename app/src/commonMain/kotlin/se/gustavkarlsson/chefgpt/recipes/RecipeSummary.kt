package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeId

data class RecipeSummary(
    val id: RecipeId,
    val title: String,
    val imageUrl: ImageUrl? = null,
    val favorite: Boolean = false,
    val modifiedFrom: RecipeId? = null,
)
