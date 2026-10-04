package se.gustavkarlsson.chefgpt.api.ingredients.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-new-ingredient")
data class ApiNewIngredient(
    val name: String,
)
