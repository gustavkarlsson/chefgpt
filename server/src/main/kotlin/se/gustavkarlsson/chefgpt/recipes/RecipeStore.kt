package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId

interface RecipeStore {
    suspend fun getRecipes(userId: UserId): List<ApiRecipe>

    fun streamRecipes(userId: UserId): Flow<List<ApiRecipe>>

    suspend fun addRecipe(
        userId: UserId,
        title: String,
        url: String,
        imageUrl: String?,
    ): ApiRecipe

    // Returns true if a recipe was removed, false if no recipe matched.
    suspend fun deleteRecipe(
        userId: UserId,
        id: RecipeId,
    ): Boolean
}
