package se.gustavkarlsson.chefgpt.db

import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

fun connectR2dbcDatabase(config: ApplicationConfig): R2dbcDatabase {
    val jdbcUrl = config.property("url").getString()
    val r2dbcUrl = jdbcUrl.replaceFirst("jdbc:", "r2dbc:")
    return R2dbcDatabase.connect(r2dbcUrl)
}
