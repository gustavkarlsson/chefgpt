package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeId
import kotlin.time.Duration

data class Recipe(
    val id: RecipeId,
    val title: String,
    val imageUrl: ImageUrl? = null,
    val description: String? = null,
    val preparationDuration: Duration? = null,
    val cookingDuration: Duration? = null,
    val duration: Duration? = null,
    val steps: List<String>,
    val ingredients: List<Ingredient> = emptyList(),
    val nutrients: List<Nutrient> = emptyList(),
)

data class Ingredient(
    val name: String,
    val amount: String,
)

data class Nutrient(
    val name: String,
    val value: String,
)
