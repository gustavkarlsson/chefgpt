package se.gustavkarlsson.chefgpt.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import java.util.Properties

fun createHikariDataSource(config: ApplicationConfig): HikariDataSource {
    val propertiesConfig = config.toHikariConfig()
    val hikariConfig = HikariConfig(propertiesConfig)
    return HikariDataSource(hikariConfig)
}

private fun ApplicationConfig.toHikariConfig(): Properties {
    val properties = Properties()
    val propertyMap = toMap().toPropertyList()
    for ((key, value) in propertyMap) {
        properties[key] = value
    }
    return properties
}

private fun Map<String, Any?>.toPropertyList(): List<Pair<String, String>> =
    this.flatMap { (key, value) ->
        when (value) {
            is Map<*, *> -> value.mapKeys { (subKey, _) -> "$key.$subKey" }.toPropertyList()
            is List<*> -> error("List value cannot be converted to property value: $key=$value")
            else -> listOf(key to value.toString())
        }
    }
