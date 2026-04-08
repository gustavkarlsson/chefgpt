package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.auth.UserId

interface IngredientStore {
    suspend fun getIngredients(userId: UserId): List<String>

    fun streamIngredients(userId: UserId): Flow<List<String>>

    suspend fun addIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<String>

    suspend fun removeIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<String>

    suspend fun clearIngredients(userId: UserId): List<String>
}
