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
    val url = getDatabaseUrl("jdbc", config)
    return PGSimpleDataSource().apply {
        setUrl(url)
    }
}
