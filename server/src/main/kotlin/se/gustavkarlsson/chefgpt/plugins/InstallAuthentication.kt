package se.gustavkarlsson.chefgpt.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.session
import io.ktor.server.response.respond
import se.gustavkarlsson.chefgpt.auth.Session
import se.gustavkarlsson.chefgpt.auth.UserRepository

fun Application.installAuthentication(userRepository: UserRepository) {
    authentication {
        session<Session> {
            validate { session ->
                session.takeIf { it.user.name in userRepository }
            }
            challenge { call.respond(HttpStatusCode.Unauthorized) }
        }
    }
}
