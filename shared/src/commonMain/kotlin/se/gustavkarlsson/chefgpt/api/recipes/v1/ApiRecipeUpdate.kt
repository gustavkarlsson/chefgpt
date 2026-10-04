package se.gustavkarlsson.chefgpt.api.recipes.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-recipe-update")
data class ApiRecipeUpdate(
    val favorite: Boolean,
)
