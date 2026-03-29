package se.gustavkarlsson.chefgpt.db

import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

fun connectR2dbcDatabase(config: ApplicationConfig): R2dbcDatabase =
    R2dbcDatabase.connect(
        url = getUrl(config),
        user = config.property("username").getString(),
        password = config.property("password").getString(),
    )

private fun getUrl(config: ApplicationConfig): String =
    buildString {
        append("r2dbc:postgresql://")
        append(config.property("host").getString())
        append(":")
        append(config.property("port").getString())
        append("/")
        append(config.property("name").getString())
    }
