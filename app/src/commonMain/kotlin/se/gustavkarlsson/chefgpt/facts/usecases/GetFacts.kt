package se.gustavkarlsson.chefgpt.facts.usecases

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.facts.FactsRepository
import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface GetFacts {
    suspend operator fun invoke(sessionId: SessionId): Result<UserFacts, ClientError>
}

class HttpGetFacts(
    private val repository: FactsRepository,
) : GetFacts {
    override suspend fun invoke(sessionId: SessionId): Result<UserFacts, ClientError> = repository.get(sessionId)
}
