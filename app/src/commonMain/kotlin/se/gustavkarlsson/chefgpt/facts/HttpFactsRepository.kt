package se.gustavkarlsson.chefgpt.facts

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.sessions.SessionId

class HttpFactsRepository(
    private val client: ChefGptClient,
) : FactsRepository {
    override suspend fun get(sessionId: SessionId): Result<UserFacts, ClientError> =
        client.getFacts(sessionId).map { it.toDomainFacts() }

    override suspend fun set(
        sessionId: SessionId,
        facts: UserFacts,
    ): Result<Unit, ClientError> = client.putFacts(sessionId, facts.toApi()).map { Unit }
}
