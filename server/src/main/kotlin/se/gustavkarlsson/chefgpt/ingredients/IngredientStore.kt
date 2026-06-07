package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.auth.UserId

interface IngredientStore {
    suspend fun getIngredients(userId: UserId): List<ApiIngredient>

    fun streamIngredients(userId: UserId): Flow<List<ApiIngredient>>

    // Creation is keyed by name; existing ingredients are matched and restored by name.
    suspend fun createIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient>

    // Sets the store membership of the given existing ingredients by id. Returns
    // the updated ingredients, skipping any ids that don't belong to the user.
    suspend fun setInventory(
        userId: UserId,
        ids: List<IngredientId>,
        inInventory: Boolean,
    ): List<ApiIngredient>

    suspend fun destroyIngredients(
        userId: UserId,
        ids: List<IngredientId>,
    ): List<ApiIngredient>
}
