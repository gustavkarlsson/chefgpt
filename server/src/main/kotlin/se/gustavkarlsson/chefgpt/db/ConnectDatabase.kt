package se.gustavkarlsson.chefgpt.db

import app.cash.sqldelight.driver.r2dbc.R2dbcDriver
import io.ktor.server.config.ApplicationConfig
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory

fun connectDatabase(config: ApplicationConfig): ChefGptDatabase {
    val connectionFactory =
        PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration
                .builder()
                .host(config.property("host").getString())
                .port(config.property("port").getString().toInt())
                .database(config.property("name").getString())
                .username(config.property("username").getString())
                .password(config.property("password").getString())
                .build(),
        )
    val connection = checkNotNull(connectionFactory.create().block()) { "Failed to create database connection" }
    val driver = R2dbcDriver(connection)
    return ChefGptDatabase(driver)
}
