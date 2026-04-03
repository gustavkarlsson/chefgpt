package se.gustavkarlsson.chefgpt.sessions

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onOk
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ErrorResponse

class SessionRepositoryImpl(
    private val client: ChefGptClient,
    private val lastSessionFileStore: LastSessionFileStore,
) : SessionRepository {
    override suspend fun getCurrentSession(): SessionCredentials? = lastSessionFileStore.load() // TODO Error handling

    override suspend fun register(credentials: UserCredentials): Result<SessionCredentials, ErrorResponse> =
        client
            .register(credentials)
            .map { SessionCredentials(credentials.userName, it) }
            .onOk { lastSessionFileStore.save(it) }

    override suspend fun login(credentials: UserCredentials): Result<SessionCredentials, ErrorResponse> =
        client
            .login(credentials)
            .map { SessionCredentials(credentials.userName, it) }
            .onOk { lastSessionFileStore.save(it) }

    override suspend fun logOut(): Boolean = lastSessionFileStore.clear()
}
