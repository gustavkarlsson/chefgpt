package se.gustavkarlsson.chefgpt.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.agent.tools.models.ToolIngredient
import se.gustavkarlsson.chefgpt.agent.tools.models.toTool
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

class GetIngredientsTool(
    private val store: IngredientStore,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Get all user's ingredients, including those no longer in inventory (inInventory = false)")
    suspend fun getIngredients(): List<ToolIngredient> = store.getIngredients(userId).map { it.toTool() }
}

class AddIngredientsTool(
    private val store: IngredientStore,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Mark the given ingredients as in the user's inventory. Returns the ingredients whose status actually changed, excluding any that were already in inventory",
    )
    suspend fun addIngredients(ingredients: List<String>): List<ToolIngredient> =
        store.createIngredients(userId, ingredients).map { it.toTool() }
}

class RemoveIngredientsTool(
    private val store: IngredientStore,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Mark the given ingredients as no longer in the user's inventory, keeping them in the store. Returns the affected ingredients",
    )
    suspend fun removeIngredients(ingredients: List<String>): List<ToolIngredient> =
        store.setInventory(userId, resolveIds(store, userId, ingredients), inInventory = false).map { it.toTool() }
}

class DestroyIngredientsTool(
    private val store: IngredientStore,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Permanently delete the given ingredients from the user's store. Returns the ingredients that were actually deleted, excluding any that did not exist",
    )
    suspend fun destroyIngredients(ingredients: List<String>): List<ToolIngredient> =
        store.destroyIngredients(userId, resolveIds(store, userId, ingredients)).map { it.toTool() }
}

private suspend fun resolveIds(
    store: IngredientStore,
    userId: UserId,
    names: List<String>,
): List<IngredientId> {
    val normalized = names.map { it.trim().lowercase() }.toSet()
    return store.getIngredients(userId).filter { it.name in normalized }.map { it.id }
}
