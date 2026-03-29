package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.header
import se.gustavkarlsson.chefgpt.auth.Session

fun Application.installSessions(sessionStorage: SessionStorage) {
    install(Sessions) {
        header<Session>("Session-Id", sessionStorage)
    }
}
