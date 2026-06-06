package se.gustavkarlsson.chefgpt.ingredients

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.auth.UserId

@Suppress("unused")
class IngredientStoreTools(
    private val store: IngredientStore,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Get all user's ingredients, including those no longer in inventory (inInventory = false)")
    suspend fun getIngredients(): List<ApiIngredient> = store.getIngredients(userId)

    @Tool
    @LLMDescription(
        "Mark the given ingredients as in the user's inventory. Returns the ingredients whose status actually changed, excluding any that were already in inventory",
    )
    suspend fun addIngredients(ingredients: List<String>): List<ApiIngredient> =
        store.addIngredients(userId, ingredients)

    @Tool
    @LLMDescription(
        "Mark the given ingredients as no longer in the user's inventory, keeping them in the store. Returns the ingredients whose status actually changed, excluding any that were not in inventory",
    )
    suspend fun removeIngredients(ingredients: List<String>): List<ApiIngredient> =
        store.removeIngredients(userId, resolveIds(ingredients))

    @Tool
    @LLMDescription(
        "Permanently delete the given ingredients from the user's store. Returns the ingredients that were actually deleted, excluding any that did not exist",
    )
    suspend fun destroyIngredients(ingredients: List<String>): List<ApiIngredient> =
        store.destroyIngredients(userId, resolveIds(ingredients))

    private suspend fun resolveIds(names: List<String>): List<IngredientId> {
        val normalized = names.map { it.trim().lowercase() }.toSet()
        return store.getIngredients(userId).filter { it.name in normalized }.map { it.id }
    }
}

fun IngredientStore.toTools(userId: UserId): ToolSet = IngredientStoreTools(this, userId)
