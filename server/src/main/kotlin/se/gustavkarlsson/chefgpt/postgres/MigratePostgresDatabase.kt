package se.gustavkarlsson.chefgpt.postgres

import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

fun migratePostgresDatabase(config: ApplicationConfig) {
    val dataSource = createSimpleDataSource(config)
    Flyway
        .configure()
        .validateMigrationNaming(true)
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
        append(config.property("database").getString())
        val params =
            buildList {
                config.propertyOrNull("username")?.getString()?.let { add("user=$it") }
                config.propertyOrNull("password")?.getString()?.let { add("password=$it") }
            }
        if (params.isNotEmpty()) {
            append("?")
            append(params.joinToString("&"))
        }
    }
