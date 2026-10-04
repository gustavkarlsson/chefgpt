package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.common.ImageUrl
import se.gustavkarlsson.chefgpt.api.common.SpoonacularId
import kotlin.time.Duration

/** A recipe about to be saved, before it has an ID. */
data class NewRecipe(
    val title: String,
    val steps: List<String>,
    val imageUrl: ImageUrl?,
    val description: String?,
    val preparationDuration: Duration?,
    val cookingDuration: Duration?,
    val duration: Duration?,
    val servings: IntRange?,
    val ingredients: List<RecipeIngredient>,
    val nutrients: List<Nutrient>,
    // Only a recipe looked up from Spoonacular has one; the agent writes recipes without.
    val spoonacularId: SpoonacularId?,
)
