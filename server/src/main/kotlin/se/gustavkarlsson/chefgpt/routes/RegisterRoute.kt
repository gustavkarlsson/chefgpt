package se.gustavkarlsson.chefgpt.routes

import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.ResponseData
import se.gustavkarlsson.chefgpt.api.auth.v1.AUTH_V1_PATH
import se.gustavkarlsson.chefgpt.api.errors.v1.ApiError
import se.gustavkarlsson.chefgpt.auth.RegistrationError
import se.gustavkarlsson.chefgpt.auth.Session
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.auth.toCookie
import se.gustavkarlsson.chefgpt.getCredentials
import se.gustavkarlsson.chefgpt.respond

fun Routing.registerRoute() {
    val userRepository = get<UserRepository>()
    post("$AUTH_V1_PATH/register") {
        call
            .getCredentials()
            .flatMap { credentials ->
                userRepository.register(credentials.name, credentials.password).mapError { registrationError ->
                    when (registrationError) {
                        is RegistrationError.InvalidUserName -> {
                            ResponseData(
                                status = HttpStatusCode.BadRequest,
                                body = ApiError("invalid-username", registrationError.message, userMessage = null),
                            )
                        }

                        is RegistrationError.InvalidPassword -> {
                            ResponseData(
                                status = HttpStatusCode.BadRequest,
                                body = ApiError("invalid-password", registrationError.message, userMessage = null),
                            )
                        }

                        RegistrationError.UsernameTaken -> {
                            ResponseData(HttpStatusCode.Conflict)
                        }
                    }
                }
            }.map { user ->
                Session(user).toCookie()
            }.onOk { session ->
                call.sessions.set(session)
            }.map {
                ResponseData(status = HttpStatusCode.NoContent)
            }.respond(call)
    }
}
