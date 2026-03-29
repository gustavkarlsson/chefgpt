package se.gustavkarlsson.chefgpt.db

import io.ktor.server.config.ApplicationConfig
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

fun createSimpleDataSource(config: ApplicationConfig): DataSource {
    val url = config.property("url").getString()
    return PGSimpleDataSource().apply {
        setUrl(url)
    }
}
