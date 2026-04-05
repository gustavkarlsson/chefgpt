package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
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
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess
import se.gustavkarlsson.chefgpt.postgres.migratePostgresDatabase

fun Application.createPostgresModule() =
    module {
        val config = environment.config
        when (val storage = config.property("bindings.storage").getString()) {
            "database" -> {
                // FIXME Consider not providing "access",
                //  but the database connection itself (lazily) through a request scope.
                single {
                    val databaseConfig = config.config("postgres")
                    migratePostgresDatabase(databaseConfig)
                    createPostgresAccess(databaseConfig)
                }
            }

            "memory" -> {
                Unit
            }

            else -> {
                error("bindings.storage must be 'memory' or 'database', got '$storage'")
            }
        }
    }

private fun createPostgresAccess(config: ApplicationConfig): PostgresAccess {
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
    return PostgresAccess(connectionFactory)
}
