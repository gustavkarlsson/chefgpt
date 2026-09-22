package se.gustavkarlsson.chefgpt.recipes.usecases

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.recipes.Recipe
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface GetRecipe {
    suspend operator fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError>
}

class HttpGetRecipe(
    private val repository: RecipeRepository,
) : GetRecipe {
    override suspend fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError> = repository.get(sessionId, recipeId)
}
