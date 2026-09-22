package se.gustavkarlsson.chefgpt.ingredients.usecases

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface SetIngredientInventory {
    suspend operator fun invoke(
        sessionId: SessionId,
        ingredientId: IngredientId,
        inInventory: Boolean,
    ): Result<Unit, ClientError>
}

class HttpSetIngredientInventory(
    private val client: ChefGptClient,
) : SetIngredientInventory {
    override suspend fun invoke(
        sessionId: SessionId,
        ingredientId: IngredientId,
        inInventory: Boolean,
    ): Result<Unit, ClientError> = client.setIngredientInventory(sessionId, ingredientId, inInventory)
}
