package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import kotlin.time.Duration

data class NewRecipe(
    val title: String,
    val steps: List<String>,
    val spoonacularId: SpoonacularId? = null,
    val imageUrl: ImageUrl? = null,
    val description: String? = null,
    val preparationDuration: Duration? = null,
    val cookingDuration: Duration? = null,
    val duration: Duration? = null,
    val ingredients: List<ApiRecipeIngredient> = emptyList(),
    val nutrients: List<ApiNutrient> = emptyList(),
)

fun NewRecipe.toApiRecipe(
    id: RecipeId,
    favorite: Boolean,
    modifiedFrom: RecipeId? = null,
): ApiRecipe =
    ApiRecipe(
        id = id,
        spoonacularId = spoonacularId,
        title = title,
        imageUrl = imageUrl,
        steps = steps,
        favorite = favorite,
        modifiedFrom = modifiedFrom,
        description = description,
        preparationDuration = preparationDuration,
        cookingDuration = cookingDuration,
        duration = duration,
        ingredients = ingredients,
        nutrients = nutrients,
    )

fun ApiRecipe.toNewRecipe(): NewRecipe =
    NewRecipe(
        title = title,
        steps = steps,
        spoonacularId = spoonacularId,
        imageUrl = imageUrl,
        description = description,
        preparationDuration = preparationDuration,
        cookingDuration = cookingDuration,
        duration = duration,
        ingredients = ingredients,
        nutrients = nutrients,
    )
