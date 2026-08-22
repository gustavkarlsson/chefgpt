package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap

class InMemoryRecipeSummaryStore(
    private val storage: ConcurrentHashMap<UserId, MutableStateFlow<Map<RecipeId, ApiRecipeSummary>>> =
        ConcurrentHashMap(),
) : RecipeSummaryStore {
    override suspend fun getRecipeSummaries(userId: UserId): List<ApiRecipeSummary> =
        storage[userId]
            ?.value
            ?.values
            ?.toList()
            .orEmpty()

    override suspend fun getRecipeSummary(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipeSummary? = storage[userId]?.value?.get(id)

    override fun streamRecipeSummaries(userId: UserId): Flow<List<ApiRecipeSummary>> =
        storedRecipeSummaries(userId).map {
            it.values.toList()
        }

    override suspend fun addRecipeSummary(
        userId: UserId,
        title: String,
        spoonacularId: SpoonacularId,
        imageUrl: String?,
    ): ApiRecipeSummary {
        val recipeSummary =
            ApiRecipeSummary(
                id = RecipeId.random(),
                title = title,
                spoonacularId = spoonacularId,
                imageUrl = imageUrl,
            )
        storedRecipeSummaries(userId).updateAndGet { it + (recipeSummary.id to recipeSummary) }
        return recipeSummary
    }

    override suspend fun deleteRecipeSummary(
        userId: UserId,
        id: RecipeId,
    ): Boolean {
        var removed = false
        storedRecipeSummaries(userId).updateAndGet { current ->
            removed = current.containsKey(id)
            current - id
        }
        return removed
    }

    private fun storedRecipeSummaries(userId: UserId): MutableStateFlow<Map<RecipeId, ApiRecipeSummary>> =
        storage.getOrPut(userId) { MutableStateFlow(emptyMap()) }
}
