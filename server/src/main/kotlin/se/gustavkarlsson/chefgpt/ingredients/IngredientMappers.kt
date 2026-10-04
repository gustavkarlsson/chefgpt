package se.gustavkarlsson.chefgpt.ingredients

import se.gustavkarlsson.chefgpt.api.ingredients.v1.ApiIngredient

fun Ingredient.toApi(): ApiIngredient = ApiIngredient(id, name, lastModified, inInventory)
