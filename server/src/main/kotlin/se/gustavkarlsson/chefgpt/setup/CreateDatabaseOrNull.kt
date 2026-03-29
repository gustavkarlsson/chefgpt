package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import se.gustavkarlsson.chefgpt.db.connectR2dbcDatabase
import se.gustavkarlsson.chefgpt.db.migrateDatabase

fun createDatabaseOrNull(config: ApplicationConfig): R2dbcDatabase? =
    when (val storage = config.property("chefgpt.storage").getString()) {
        "database" -> {
            val databaseConfig = config.config("database")
            migrateDatabase(databaseConfig)
            connectR2dbcDatabase(databaseConfig)
        }

        "memory" -> {
            null
        }

        else -> {
            error("chefgpt.storage must be 'memory' or 'database', got '$storage'")
        }
    }
