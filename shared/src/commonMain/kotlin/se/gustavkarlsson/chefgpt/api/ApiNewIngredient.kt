package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-new-ingredient")
data class ApiNewIngredient(
    val name: String,
)
