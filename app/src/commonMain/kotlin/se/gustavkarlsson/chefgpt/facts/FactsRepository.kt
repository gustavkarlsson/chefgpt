package se.gustavkarlsson.chefgpt.facts

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.sessions.SessionId

interface FactsRepository {
    suspend fun get(sessionId: SessionId): Result<UserFacts, ClientError>

    suspend fun set(
        sessionId: SessionId,
        facts: UserFacts,
    ): Result<Unit, ClientError>
}
