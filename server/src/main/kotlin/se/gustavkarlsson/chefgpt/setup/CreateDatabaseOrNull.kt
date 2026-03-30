package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase
import se.gustavkarlsson.chefgpt.db.connectDatabase
import se.gustavkarlsson.chefgpt.db.migrateDatabase

fun createDatabaseOrNull(config: ApplicationConfig): ChefGptDatabase? =
    when (val storage = config.property("chefgpt.storage").getString()) {
        "database" -> {
            val databaseConfig = config.config("database")
            migrateDatabase(databaseConfig)
            connectDatabase(databaseConfig)
        }

        "memory" -> {
            null
        }

        else -> {
            error("chefgpt.storage must be 'memory' or 'database', got '$storage'")
        }
    }
