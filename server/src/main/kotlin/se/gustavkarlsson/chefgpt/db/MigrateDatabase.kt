package se.gustavkarlsson.chefgpt.db

import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun migrateDatabase(dataSource: DataSource) {
    Flyway
        .configure()
        .dataSource(dataSource)
        .load()
        .migrate()
}
