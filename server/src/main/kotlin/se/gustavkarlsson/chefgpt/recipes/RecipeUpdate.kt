package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ImageUrl
import kotlin.time.Duration

/**
 * A partial recipe update, where null means "leave unchanged". Lists are replaced
 * wholesale when set. Clearing an already set field is not supported.
 */
data class RecipeUpdate(
    val title: String? = null,
    val imageUrl: ImageUrl? = null,
    val description: String? = null,
    val preparationDuration: Duration? = null,
    val cookingDuration: Duration? = null,
    val duration: Duration? = null,
    val servings: IntRange? = null,
    val steps: List<String>? = null,
    val ingredients: List<ApiRecipeIngredient>? = null,
    val nutrients: List<ApiNutrient>? = null,
)

fun ApiRecipe.applyUpdate(update: RecipeUpdate): ApiRecipe =
    copy(
        title = update.title ?: title,
        imageUrl = update.imageUrl ?: imageUrl,
        description = update.description ?: description,
        preparationDuration = update.preparationDuration ?: preparationDuration,
        cookingDuration = update.cookingDuration ?: cookingDuration,
        duration = update.duration ?: duration,
        servings = update.servings ?: servings,
        steps = update.steps ?: steps,
        ingredients = update.ingredients ?: ingredients,
        nutrients = update.nutrients ?: nutrients,
    )
