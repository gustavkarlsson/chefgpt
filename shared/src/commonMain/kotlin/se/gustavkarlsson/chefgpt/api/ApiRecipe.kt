package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.DurationSerializer
import kotlin.time.Duration

@Serializable
data class ApiRecipe(
    val id: RecipeId,
    val spoonacularId: SpoonacularId,
    val title: String,
    val imageUrl: ImageUrl? = null,
    val steps: List<String>,
    val description: String? = null,
    @Serializable(with = DurationSerializer::class)
    val preparationDuration: Duration? = null,
    @Serializable(with = DurationSerializer::class)
    val cookingDuration: Duration? = null,
    @Serializable(with = DurationSerializer::class)
    val duration: Duration? = null,
    val ingredients: List<ApiRecipeIngredient> = emptyList(),
    val nutrients: List<ApiNutrient> = emptyList(),
)

@Serializable
data class ApiRecipeIngredient(
    val name: String,
    val value: String,
    val unit: String,
)

@Serializable
data class ApiNutrient(
    val name: String,
    val value: String,
    val unit: String,
)
