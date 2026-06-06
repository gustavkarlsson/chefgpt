package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.auth.UserId

interface IngredientStore {
    suspend fun getIngredients(userId: UserId): List<ApiIngredient>

    fun streamIngredients(userId: UserId): Flow<List<ApiIngredient>>

    suspend fun addIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient>

    suspend fun removeIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient>

    suspend fun destroyIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient>
}
