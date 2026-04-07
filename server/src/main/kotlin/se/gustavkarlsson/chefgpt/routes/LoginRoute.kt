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
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.auth.LoginError
import se.gustavkarlsson.chefgpt.auth.Session
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.getCredentials
import se.gustavkarlsson.chefgpt.respond

fun Routing.loginRoute() {
    post("/login") {
        val userRepository = get<UserRepository>()
        call
            .getCredentials()
            .flatMap { credentials ->
                userRepository.login(credentials.name, credentials.password).mapError { loginError ->
                    when (loginError) {
                        LoginError.WrongCredentials -> {
                            ResponseData(
                                status = HttpStatusCode.Unauthorized,
                                body = ApiError("wrong-credentials", "Wrong credentials"),
                            )
                        }
                    }
                }
            }.map { user ->
                Session(user)
            }.onOk { session ->
                call.sessions.set(session)
            }.map {
                ResponseData(status = HttpStatusCode.NoContent)
            }.respond(call)
    }
}
