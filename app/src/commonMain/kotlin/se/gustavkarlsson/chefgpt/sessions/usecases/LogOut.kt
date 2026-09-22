package se.gustavkarlsson.chefgpt.sessions.usecases

import se.gustavkarlsson.chefgpt.sessions.SessionRepository

fun interface LogOut {
    suspend operator fun invoke(): Boolean
}

class HttpLogOut(
    private val repository: SessionRepository,
) : LogOut {
    override suspend fun invoke(): Boolean = repository.logOut()
}
