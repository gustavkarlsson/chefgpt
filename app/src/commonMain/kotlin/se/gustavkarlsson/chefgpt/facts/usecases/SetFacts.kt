package se.gustavkarlsson.chefgpt.facts.usecases

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.facts.FactsRepository
import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface SetFacts {
    suspend operator fun invoke(
        sessionId: SessionId,
        facts: UserFacts,
    ): Result<Unit, ClientError>
}

class HttpSetFacts(
    private val repository: FactsRepository,
) : SetFacts {
    override suspend fun invoke(
        sessionId: SessionId,
        facts: UserFacts,
    ): Result<Unit, ClientError> = repository.set(sessionId, facts)
}
