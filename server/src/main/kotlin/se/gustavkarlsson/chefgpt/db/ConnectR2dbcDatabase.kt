package se.gustavkarlsson.chefgpt.db

import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

fun connectR2dbcDatabase(config: ApplicationConfig): R2dbcDatabase {
    val url = getDatabaseUrl("r2dbc", config)
    return R2dbcDatabase.connect(url)
}
