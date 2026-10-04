package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeId

fun Recipe.toApi(): ApiRecipe =
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
        ingredients = ingredients.map { ApiRecipeIngredient(it.name, it.value, it.unit) },
        nutrients = nutrients.map { ApiNutrient(it.name, it.value, it.unit) },
    )

fun RecipeSummary.toApi(): ApiRecipeSummary =
    ApiRecipeSummary(
        id = id,
        title = title,
        spoonacularId = spoonacularId,
        imageUrl = imageUrl,
        favorite = favorite,
        modifiedFrom = modifiedFrom,
    )

fun NewRecipe.toApiRecipe(
    id: RecipeId,
    favorite: Boolean,
    modifiedFrom: RecipeId? = null,
): ApiRecipe = toRecipe(id, favorite, modifiedFrom).toApi()

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
        ingredients = ingredients.map { RecipeIngredient(it.name, it.value, it.unit) },
        nutrients = nutrients.map { Nutrient(it.name, it.value, it.unit) },
    )
