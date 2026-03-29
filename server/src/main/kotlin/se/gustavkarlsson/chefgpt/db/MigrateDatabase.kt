package se.gustavkarlsson.chefgpt.db

import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

fun migrateDatabase(config: ApplicationConfig) {
    val dataSource = createSimpleDataSource(config)
    Flyway
        .configure()
        .dataSource(dataSource)
        .load()
        .migrate()
}

private fun createSimpleDataSource(config: ApplicationConfig): DataSource {
    val url = getUrl(config)
    return PGSimpleDataSource().apply {
        setUrl(url)
    }
}

private fun getUrl(config: ApplicationConfig): String =
    buildString {
        append("jdbc:postgresql://")
        append(config.property("host").getString())
        append(":")
        append(config.property("port").getString())
        append("/")
        append(config.property("name").getString())
        append("?user=")
        append(config.property("username").getString())
        append("&password=")
        append(config.property("password").getString())
    }
