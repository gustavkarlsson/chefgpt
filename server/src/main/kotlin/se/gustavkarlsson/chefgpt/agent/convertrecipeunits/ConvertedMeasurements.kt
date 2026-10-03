package se.gustavkarlsson.chefgpt.agent.convertrecipeunits

import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient

// The parts of a recipe whose units the conversion agent can convert. A null field means that
// part was not converted, and the caller should keep the recipe's original value.
data class ConvertedMeasurements(
    val ingredients: List<ApiRecipeIngredient>?,
    val description: String?,
    val steps: List<String>?,
)
