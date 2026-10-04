package se.gustavkarlsson.chefgpt.agent.tools.models

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.ingredients.Ingredient

@Serializable
@LLMDescription("An ingredient in the user's pantry.")
data class ToolIngredient(
    @property:LLMDescription("The ID of the ingredient.")
    val id: String,
    @property:LLMDescription("The name of the ingredient, e.g. 'olive oil'.")
    val name: String,
    @property:LLMDescription("Whether the ingredient is in the user's inventory right now.")
    val inInventory: Boolean,
)

fun Ingredient.toTool(): ToolIngredient = ToolIngredient(id.value.toString(), name, inInventory)
