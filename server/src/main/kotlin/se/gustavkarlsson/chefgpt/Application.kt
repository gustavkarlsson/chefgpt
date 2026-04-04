package se.gustavkarlsson.chefgpt

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import se.gustavkarlsson.chefgpt.plugins.installAuthentication
import se.gustavkarlsson.chefgpt.plugins.installCallLogging
import se.gustavkarlsson.chefgpt.plugins.installContentNegotiation
import se.gustavkarlsson.chefgpt.plugins.installKoin
import se.gustavkarlsson.chefgpt.plugins.installKoog
import se.gustavkarlsson.chefgpt.plugins.installSSE
import se.gustavkarlsson.chefgpt.plugins.installSessions

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    installKoin()
    installCallLogging()
    installContentNegotiation()
    installSSE()
    installKoog()
    installSessions()
    installAuthentication()
    routing(Routing::routes)
}
