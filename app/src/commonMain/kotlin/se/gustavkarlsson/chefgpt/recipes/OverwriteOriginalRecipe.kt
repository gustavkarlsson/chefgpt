package se.gustavkarlsson.chefgpt.recipes

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface OverwriteOriginalRecipe {
    suspend operator fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError>
}

class HttpOverwriteOriginalRecipe(
    private val repository: RecipeRepository,
) : OverwriteOriginalRecipe {
    override suspend fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError> = repository.overwriteOriginal(sessionId, recipeId)
}
