package se.gustavkarlsson.chefgpt.api.ingredients.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-ingredient-update")
data class ApiIngredientUpdate(
    val inInventory: Boolean,
)
