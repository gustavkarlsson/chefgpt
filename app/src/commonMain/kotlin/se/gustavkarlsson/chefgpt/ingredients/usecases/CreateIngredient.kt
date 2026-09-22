package se.gustavkarlsson.chefgpt.ingredients.usecases

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface CreateIngredient {
    suspend operator fun invoke(
        sessionId: SessionId,
        name: String,
    ): Result<Unit, ClientError>
}

class HttpCreateIngredient(
    private val client: ChefGptClient,
) : CreateIngredient {
    override suspend fun invoke(
        sessionId: SessionId,
        name: String,
    ): Result<Unit, ClientError> = client.createIngredient(sessionId, name)
}
