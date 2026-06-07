package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-ingredient-update")
data class ApiIngredientUpdate(
    val inInventory: Boolean,
)
