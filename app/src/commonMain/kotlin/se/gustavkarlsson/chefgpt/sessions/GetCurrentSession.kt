package se.gustavkarlsson.chefgpt.sessions

import com.github.michaelbull.result.Result

fun interface GetCurrentSession {
    suspend operator fun invoke(): Result<SessionCredentials?, Unit>
}

class HttpGetCurrentSession(
    private val repository: SessionRepository,
) : GetCurrentSession {
    override suspend fun invoke(): Result<SessionCredentials?, Unit> = repository.getCurrentSession()
}
