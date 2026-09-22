package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface StreamIngredients {
    operator fun invoke(sessionId: SessionId): Flow<List<ApiIngredient>>
}

class HttpStreamIngredients(
    private val client: ChefGptClient,
) : StreamIngredients {
    override operator fun invoke(sessionId: SessionId): Flow<List<ApiIngredient>> =
        client.listenToIngredients(sessionId)
}
