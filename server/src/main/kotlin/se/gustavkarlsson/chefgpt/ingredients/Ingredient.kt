package se.gustavkarlsson.chefgpt.ingredients

import se.gustavkarlsson.chefgpt.api.IngredientId
import kotlin.time.Instant

/** An ingredient in the user's pantry, whether or not it is currently in inventory. */
data class Ingredient(
    val id: IngredientId,
    val name: String,
    val lastModified: Instant,
    val inInventory: Boolean,
)
