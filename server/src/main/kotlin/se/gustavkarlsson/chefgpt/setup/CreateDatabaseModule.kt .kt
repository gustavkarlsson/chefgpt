package se.gustavkarlsson.chefgpt.setup

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase
import se.gustavkarlsson.chefgpt.postgres.PostgresDatabasePool
import se.gustavkarlsson.chefgpt.postgres.migratePostgresDatabase

fun Application.createDatabaseModule() =
    module {
        val config = environment.config
        when (val storage = config.property("bindings.storage").getString()) {
            "database" -> {
                single {
                    val databaseConfig = config.config("postgres")
                    migratePostgresDatabase(databaseConfig)
                    val dataSource = createHikariDataSource(databaseConfig)
                    val driver = dataSource.asJdbcDriver()
                    val database = ChefGptDatabase(driver)
                    PostgresDatabasePool(database)
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

private fun createHikariDataSource(config: ApplicationConfig): HikariDataSource {
    val host = config.property("host").getString()
    val port = config.property("port").getString()
    val database = config.property("database").getString()
    val username = config.propertyOrNull("username")?.getString()
    val password = config.propertyOrNull("password")?.getString()
    return HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://$host:$port/$database"
            if (username != null) this.username = username
            if (password != null) this.password = password
        },
    )
}
