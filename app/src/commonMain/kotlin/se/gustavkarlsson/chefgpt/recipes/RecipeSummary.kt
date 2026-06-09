package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeSummaryId

typealias RecipeId = RecipeSummaryId

data class RecipeSummary(
    val id: RecipeId,
    val title: String,
    val imageUrl: ImageUrl? = null,
)
