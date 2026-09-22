package se.gustavkarlsson.chefgpt.sessions.usecases

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.sessions.SessionCredentials
import se.gustavkarlsson.chefgpt.sessions.SessionRepository

fun interface GetCurrentSession {
    suspend operator fun invoke(): Result<SessionCredentials?, Unit>
}

class HttpGetCurrentSession(
    private val repository: SessionRepository,
) : GetCurrentSession {
    override suspend fun invoke(): Result<SessionCredentials?, Unit> = repository.getCurrentSession()
}
