package se.gustavkarlsson.chefgpt.ingredients

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface DestroyIngredient {
    suspend operator fun invoke(
        sessionId: SessionId,
        ingredientId: IngredientId,
    ): Result<Unit, ClientError>
}

class HttpDestroyIngredient(
    private val client: ChefGptClient,
) : DestroyIngredient {
    override suspend fun invoke(
        sessionId: SessionId,
        ingredientId: IngredientId,
    ): Result<Unit, ClientError> = client.destroyIngredient(sessionId, ingredientId)
}
