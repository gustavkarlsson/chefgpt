package se.gustavkarlsson.chefgpt.ingredients

import se.gustavkarlsson.chefgpt.auth.UserId

interface IngredientStore {
    suspend fun getIngredients(userId: UserId): List<String>

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
