package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.*
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions.*
import se.gustavkarlsson.chefgpt.db.DatabaseAccess
import se.gustavkarlsson.chefgpt.db.migrateDatabase

fun createDatabaseAccessOrNull(config: ApplicationConfig): DatabaseAccess? =
    when (val storage = config.property("chefgpt.storage").getString()) {
        "database" -> {
            val databaseConfig = config.config("database")
            migrateDatabase(databaseConfig)
            createDatabaseAccess(databaseConfig)
        }

        "memory" -> {
            null
        }

        else -> {
            error("chefgpt.storage must be 'memory' or 'database', got '$storage'")
        }
    }

private fun createDatabaseAccess(config: ApplicationConfig): DatabaseAccess {
    val connectionFactory =
        ConnectionFactories.get(
            builder()
                .option(DRIVER, "pool")
                .option(PROTOCOL, "postgresql")
                .option(HOST, config.property("host").getString())
                .option(PORT, config.property("port").getString().toInt())
                .option(DATABASE, config.property("name").getString())
                .option(USER, config.property("username").getString())
                .option(PASSWORD, config.property("password").getString())
                .build(),
        )
    return DatabaseAccess(connectionFactory)
}
