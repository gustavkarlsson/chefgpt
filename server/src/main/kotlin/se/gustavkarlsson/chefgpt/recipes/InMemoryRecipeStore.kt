package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap

class InMemoryRecipeStore(
    private val storage: ConcurrentHashMap<UserId, MutableStateFlow<Map<RecipeId, ApiRecipe>>> = ConcurrentHashMap(),
) : RecipeStore {
    override suspend fun getRecipes(userId: UserId): List<ApiRecipe> =
        storage[userId]
            ?.value
            ?.values
            ?.toList()
            .orEmpty()

    override fun streamRecipes(userId: UserId): Flow<List<ApiRecipe>> = storedRecipes(userId).map { it.values.toList() }

    override suspend fun addRecipe(
        userId: UserId,
        title: String,
        url: String,
        imageUrl: String?,
    ): ApiRecipe {
        val recipe = ApiRecipe(id = RecipeId.random(), title = title, url = url, imageUrl = imageUrl)
        storedRecipes(userId).updateAndGet { it + (recipe.id to recipe) }
        return recipe
    }

    override suspend fun deleteRecipe(
        userId: UserId,
        id: RecipeId,
    ): Boolean {
        var removed = false
        storedRecipes(userId).updateAndGet { current ->
            removed = current.containsKey(id)
            current - id
        }
        return removed
    }

    private fun storedRecipes(userId: UserId): MutableStateFlow<Map<RecipeId, ApiRecipe>> =
        storage.getOrPut(userId) { MutableStateFlow(emptyMap()) }
}
