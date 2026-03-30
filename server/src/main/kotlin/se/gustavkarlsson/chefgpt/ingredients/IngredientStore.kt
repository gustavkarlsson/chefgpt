package se.gustavkarlsson.chefgpt.ingredients

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

interface IngredientStore : ToolSet {
    @Tool
    @LLMDescription("Get all user's stored ingredients")
    suspend fun getIngredients(): List<String>

    @Tool
    @LLMDescription(
        "Add the given ingredients to the user's store. Returns the ingredients that were actually added, excluding any that already existed",
    )
    suspend fun addIngredients(ingredients: List<String>): List<String>

    @Tool
    @LLMDescription(
        "Remove the given ingredients from the user's store. Returns the ingredients that were actually removed, excluding any that did not exist",
    )
    suspend fun removeIngredients(ingredients: List<String>): List<String>

    @Tool
    @LLMDescription("Clear all ingredients from the user's store. Returns the ingredients that were removed")
    suspend fun clearIngredients(): List<String>
}
