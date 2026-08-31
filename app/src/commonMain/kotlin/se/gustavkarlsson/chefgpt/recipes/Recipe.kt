package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeId
import kotlin.time.Duration

data class Recipe(
    val id: RecipeId,
    val title: String,
    val imageUrl: ImageUrl?,
    val favorite: Boolean,
    val modifiedFrom: RecipeId?,
    val description: String?,
    val preparationDuration: Duration?,
    val cookingDuration: Duration?,
    val duration: Duration?,
    val servings: IntRange?,
    val steps: List<String>,
    val ingredients: List<Ingredient>,
    val nutrients: List<Nutrient>,
)

data class Ingredient(
    val name: String,
    val amount: String,
)

data class Nutrient(
    val name: String,
    val value: String,
)
