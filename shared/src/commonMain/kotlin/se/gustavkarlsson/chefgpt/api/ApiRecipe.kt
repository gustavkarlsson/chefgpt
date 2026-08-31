package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.DurationSerializer
import se.gustavkarlsson.chefgpt.IntRangeSerializer
import kotlin.time.Duration

@Serializable
data class ApiRecipe(
    val id: RecipeId,
    val spoonacularId: SpoonacularId?,
    val title: String,
    val imageUrl: ImageUrl?,
    val steps: List<String>,
    val favorite: Boolean,
    val modifiedFrom: RecipeId?,
    val description: String?,
    @Serializable(with = DurationSerializer::class)
    val preparationDuration: Duration?,
    @Serializable(with = DurationSerializer::class)
    val cookingDuration: Duration?,
    @Serializable(with = DurationSerializer::class)
    val duration: Duration?,
    @Serializable(with = IntRangeSerializer::class)
    val servings: IntRange?,
    val ingredients: List<ApiRecipeIngredient>,
    val nutrients: List<ApiNutrient>,
)

@Serializable
data class ApiRecipeIngredient(
    val name: String,
    val value: String,
    val unit: String?,
)

@Serializable
data class ApiNutrient(
    val name: String,
    val value: String,
    val unit: String?,
)
