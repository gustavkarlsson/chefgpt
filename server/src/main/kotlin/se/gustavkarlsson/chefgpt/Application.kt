package se.gustavkarlsson.chefgpt

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import org.jetbrains.annotations.VisibleForTesting
import org.koin.core.module.Module
import se.gustavkarlsson.chefgpt.plugins.installAuthentication
import se.gustavkarlsson.chefgpt.plugins.installCallLogging
import se.gustavkarlsson.chefgpt.plugins.installContentNegotiation
import se.gustavkarlsson.chefgpt.plugins.installKoin
import se.gustavkarlsson.chefgpt.plugins.installKoog
import se.gustavkarlsson.chefgpt.plugins.installRouting
import se.gustavkarlsson.chefgpt.plugins.installSSE
import se.gustavkarlsson.chefgpt.plugins.installSessions

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    moduleWithExtraKoinModules(emptyList())
}

@VisibleForTesting
fun Application.moduleWithExtraKoinModules(extraKoinModules: List<Module>) {
    installKoin(extraKoinModules)
    installCallLogging()
    installContentNegotiation()
    installSSE()
    installKoog()
    installSessions()
    installAuthentication()
    installRouting()
}
