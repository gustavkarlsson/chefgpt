package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeSummaryId
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.chefgpt.auth.UserId

interface RecipeSummaryStore {
    suspend fun getRecipeSummaries(userId: UserId): List<ApiRecipeSummary>

    suspend fun getRecipeSummary(
        userId: UserId,
        id: RecipeSummaryId,
    ): ApiRecipeSummary?

    fun streamRecipeSummaries(userId: UserId): Flow<List<ApiRecipeSummary>>

    suspend fun addRecipeSummary(
        userId: UserId,
        title: String,
        spoonacularId: SpoonacularId,
        imageUrl: String?,
    ): ApiRecipeSummary

    // Returns true if a recipe summary was removed, false if no recipe summary matched.
    suspend fun deleteRecipeSummary(
        userId: UserId,
        id: RecipeSummaryId,
    ): Boolean
}
