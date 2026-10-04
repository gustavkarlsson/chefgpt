package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.common.ImageUrl
import se.gustavkarlsson.chefgpt.api.common.RecipeId
import se.gustavkarlsson.chefgpt.api.common.SpoonacularId
import kotlin.time.Duration

/** A saved recipe. */
data class Recipe(
    val id: RecipeId,
    val spoonacularId: SpoonacularId?,
    val title: String,
    val imageUrl: ImageUrl?,
    val steps: List<String>,
    val favorite: Boolean,
    val modifiedFrom: RecipeId?,
    val description: String?,
    val preparationDuration: Duration?,
    val cookingDuration: Duration?,
    val duration: Duration?,
    val servings: IntRange?,
    val ingredients: List<RecipeIngredient>,
    val nutrients: List<Nutrient>,
)

/** An ingredient in a recipe, with its amount and unit. */
data class RecipeIngredient(
    val name: String,
    val value: String,
    val unit: String?,
)

/** A nutrient in a recipe, with its amount and unit. */
data class Nutrient(
    val name: String,
    val value: String,
    val unit: String?,
)

/** The list view of a recipe, without its steps, ingredients and nutrients. */
data class RecipeSummary(
    val id: RecipeId,
    val title: String,
    val spoonacularId: SpoonacularId?,
    val imageUrl: ImageUrl?,
    val favorite: Boolean,
    val modifiedFrom: RecipeId?,
)

fun Recipe.toSummary(): RecipeSummary =
    RecipeSummary(
        id = id,
        title = title,
        spoonacularId = spoonacularId,
        imageUrl = imageUrl,
        favorite = favorite,
        modifiedFrom = modifiedFrom,
    )

// The stored recipe becomes a new recipe when it is saved again as a modification.
fun Recipe.toNewRecipe(): NewRecipe =
    NewRecipe(
        title = title,
        steps = steps,
        imageUrl = imageUrl,
        description = description,
        preparationDuration = preparationDuration,
        cookingDuration = cookingDuration,
        duration = duration,
        servings = servings,
        ingredients = ingredients,
        nutrients = nutrients,
        spoonacularId = spoonacularId,
    )

fun NewRecipe.toRecipe(
    id: RecipeId,
    favorite: Boolean,
    modifiedFrom: RecipeId?,
): Recipe =
    Recipe(
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
