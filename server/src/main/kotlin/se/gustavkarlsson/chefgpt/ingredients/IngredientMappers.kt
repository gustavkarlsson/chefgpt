package se.gustavkarlsson.chefgpt.ingredients

import se.gustavkarlsson.chefgpt.api.ApiIngredient

fun Ingredient.toApi(): ApiIngredient = ApiIngredient(id, name, lastModified, inInventory)
