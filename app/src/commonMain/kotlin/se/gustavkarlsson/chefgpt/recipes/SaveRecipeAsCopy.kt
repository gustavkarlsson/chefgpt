package se.gustavkarlsson.chefgpt.recipes

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface SaveRecipeAsCopy {
    suspend operator fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError>
}

class HttpSaveRecipeAsCopy(
    private val repository: RecipeRepository,
) : SaveRecipeAsCopy {
    override suspend fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError> = repository.saveAsCopy(sessionId, recipeId)
}
