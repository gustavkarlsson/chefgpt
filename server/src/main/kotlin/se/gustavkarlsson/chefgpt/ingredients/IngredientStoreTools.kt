package se.gustavkarlsson.chefgpt.ingredients

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.auth.UserId

@Suppress("unused")
class IngredientStoreTools(
    private val store: IngredientStore,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Get all user's stored ingredients")
    suspend fun getIngredients(): List<String> = store.getIngredients(userId)

    @Tool
    @LLMDescription(
        "Add the given ingredients to the user's store. Returns the ingredients that were actually added, excluding any that already existed",
    )
    suspend fun addIngredients(ingredients: List<String>): List<String> = store.addIngredients(userId, ingredients)

    @Tool
    @LLMDescription(
        "Remove the given ingredients from the user's store. Returns the ingredients that were actually removed, excluding any that did not exist",
    )
    suspend fun removeIngredients(ingredients: List<String>): List<String> =
        store.removeIngredients(userId, ingredients)

    @Tool
    @LLMDescription("Clear all ingredients from the user's store. Returns the ingredients that were removed")
    suspend fun clearIngredients(): List<String> = store.clearIngredients(userId)
}

fun IngredientStore.toTools(userId: UserId): ToolSet = IngredientStoreTools(this, userId)
