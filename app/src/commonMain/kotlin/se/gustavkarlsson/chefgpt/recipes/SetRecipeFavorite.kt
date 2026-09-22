package se.gustavkarlsson.chefgpt.recipes

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface SetRecipeFavorite {
    suspend operator fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
        favorite: Boolean,
    ): Result<Unit, ClientError>
}

class HttpSetRecipeFavorite(
    private val repository: RecipeRepository,
) : SetRecipeFavorite {
    override suspend fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
        favorite: Boolean,
    ): Result<Unit, ClientError> = repository.setFavorite(sessionId, recipeId, favorite)
}
