package se.gustavkarlsson.chefgpt.recipes

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.sessions.SessionId

interface RecipeRepository {
    suspend fun streamSummaries(sessionId: SessionId): Flow<List<RecipeSummary>>

    suspend fun get(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError>

    suspend fun delete(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Unit, ClientError>
}
