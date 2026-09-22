package se.gustavkarlsson.chefgpt.recipes

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface DeleteRecipe {
    suspend operator fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Unit, ClientError>
}

class HttpDeleteRecipe(
    private val repository: RecipeRepository,
) : DeleteRecipe {
    override suspend fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Unit, ClientError> = repository.delete(sessionId, recipeId)
}
