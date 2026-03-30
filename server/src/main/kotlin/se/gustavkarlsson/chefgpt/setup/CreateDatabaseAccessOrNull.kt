package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions.DATABASE
import io.r2dbc.spi.ConnectionFactoryOptions.DRIVER
import io.r2dbc.spi.ConnectionFactoryOptions.HOST
import io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD
import io.r2dbc.spi.ConnectionFactoryOptions.PORT
import io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL
import io.r2dbc.spi.ConnectionFactoryOptions.USER
import io.r2dbc.spi.ConnectionFactoryOptions.builder
import se.gustavkarlsson.chefgpt.db.DatabaseAccess
import se.gustavkarlsson.chefgpt.db.migrateDatabase

fun createDatabaseAccessOrNull(config: ApplicationConfig): DatabaseAccess? =
    when (val storage = config.property("chefgpt.storage").getString()) {
        "database" -> {
            val databaseConfig = config.config("postgres")
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
    val username = config.propertyOrNull("username")?.getString()
    val password = config.propertyOrNull("password")?.getString()
    val connectionFactory =
        ConnectionFactories.get(
            builder()
                .option(DRIVER, "pool")
                .option(PROTOCOL, "postgresql")
                .option(HOST, config.property("host").getString())
                .option(PORT, config.property("port").getString().toInt())
                .option(DATABASE, config.property("database").getString())
                .apply {
                    if (username != null) option(USER, username)
                    if (password != null) option(PASSWORD, password)
                }.build(),
        )
    return DatabaseAccess(connectionFactory)
}
