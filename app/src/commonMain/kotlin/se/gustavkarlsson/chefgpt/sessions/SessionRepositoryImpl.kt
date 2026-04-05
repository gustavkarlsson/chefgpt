package se.gustavkarlsson.chefgpt.sessions

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onOk
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ErrorResponse

private val log = Logger.withTag("${SessionRepositoryImpl::class.simpleName}")

class SessionRepositoryImpl(
    private val client: ChefGptClient,
    private val lastSessionFileStore: LastSessionFileStore,
) : SessionRepository {
    override suspend fun getCurrentSession(): Result<SessionCredentials?, Unit> = lastSessionFileStore.load()

    override suspend fun register(credentials: UserCredentials): Result<SessionCredentials, RegisterError> =
        client
            .register(credentials)
            .onOk {
                log.i { "Registered user ${credentials.userName}" }
            }.map { SessionCredentials(credentials.userName, it) }
            .mapError { RegisterError.ServerError(it) }
            .flatMap { credentials ->
                if (lastSessionFileStore.save(credentials)) {
                    Ok(credentials)
                } else {
                    Err(RegisterError.StorageFailed)
                }
            }

    override suspend fun login(credentials: UserCredentials): Result<SessionCredentials, ErrorResponse> =
        client
            .login(credentials)
            .map { SessionCredentials(credentials.userName, it) }
            .onOk { lastSessionFileStore.save(it) }

    override suspend fun logOut(): Boolean = lastSessionFileStore.clear()
}
