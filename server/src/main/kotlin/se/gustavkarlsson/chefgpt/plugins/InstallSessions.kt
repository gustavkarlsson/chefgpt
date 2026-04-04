package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.header
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.auth.Session

fun Application.installSessions() {
    val storage = get<SessionStorage>()
    install(Sessions) {
        header<Session>("Session-Id", storage)
    }
}
