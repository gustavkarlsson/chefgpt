package se.gustavkarlsson.chefgpt

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    plugins(environment.config)
    routing(Routing::routes)
    bootstrap()
}

private fun Application.bootstrap() {
    val dataSource: DataSource by dependencies
    Database.connect(dataSource)
}
