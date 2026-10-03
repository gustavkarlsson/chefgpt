package se.gustavkarlsson.chefgpt.agent.convertrecipeunits

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient

// The parts of a recipe whose units the conversion agent can convert. A null field means that
// part was not converted, and the caller should keep the recipe's original value.
@Serializable
data class ConvertedMeasurements(
    val ingredients: List<ApiRecipeIngredient>?,
    val description: String?,
    val steps: List<String>?,
)

// Structured outputs must have an object at the root for native JSON-schema mode, so each part is
// wrapped in its own object rather than requested as a bare list or string.
@Serializable
data class ConvertedIngredients(
    val ingredients: List<ApiRecipeIngredient>,
)

@Serializable
data class ConvertedDescription(
    val description: String,
)

@Serializable
data class ConvertedSteps(
    val steps: List<String>,
)
