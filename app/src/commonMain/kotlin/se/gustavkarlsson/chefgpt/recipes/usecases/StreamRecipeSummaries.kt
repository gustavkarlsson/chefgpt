package se.gustavkarlsson.chefgpt.recipes.usecases

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.RecipeSummary
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface StreamRecipeSummaries {
    suspend operator fun invoke(sessionId: SessionId): Flow<List<RecipeSummary>>
}

class HttpStreamRecipeSummaries(
    private val repository: RecipeRepository,
) : StreamRecipeSummaries {
    override suspend fun invoke(sessionId: SessionId): Flow<List<RecipeSummary>> = repository.streamSummaries(sessionId)
}
