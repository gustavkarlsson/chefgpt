package se.gustavkarlsson.chefgpt.recipes

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.sessions.SessionId

private val log = Logger.withTag("${ApiRecipeRepository::class.simpleName}")

class ApiRecipeRepository(
    private val client: ChefGptClient,
) : RecipeRepository {
    override suspend fun streamSummaries(sessionId: SessionId): Flow<List<RecipeSummary>> =
        client
            .listenToRecipeSummaries(sessionId)
            .map { summaries -> summaries.map { it.toRecipeSummary() } }

    override suspend fun delete(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Unit, ClientError> =
        client
            .deleteRecipeSummary(sessionId, recipeId)
            .onOk { log.i { "Deleted recipe: $recipeId" } }
            .onErr { log.e { "Failed to delete recipe: $recipeId" } }
}

private fun ApiRecipeSummary.toRecipeSummary(): RecipeSummary = RecipeSummary(id, title, imageUrl?.let(::ImageUrl))
