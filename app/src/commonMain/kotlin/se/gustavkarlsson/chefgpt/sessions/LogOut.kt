package se.gustavkarlsson.chefgpt.sessions

fun interface LogOut {
    suspend operator fun invoke(): Boolean
}

class HttpLogOut(
    private val repository: SessionRepository,
) : LogOut {
    override suspend fun invoke(): Boolean = repository.logOut()
}
