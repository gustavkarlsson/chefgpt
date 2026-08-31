package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.api.SpoonacularId
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
    val ingredients: List<ApiRecipeIngredient>,
    val nutrients: List<ApiNutrient>,
    // Only a recipe looked up from Spoonacular has one; the agent writes recipes without.
    val spoonacularId: SpoonacularId?,
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
        servings = servings,
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
        servings = servings,
        ingredients = ingredients,
        nutrients = nutrients,
    )
