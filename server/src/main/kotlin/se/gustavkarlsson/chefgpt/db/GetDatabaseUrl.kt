package se.gustavkarlsson.chefgpt.db

import io.ktor.server.config.ApplicationConfig

fun getDatabaseUrl(
    connection: String,
    config: ApplicationConfig,
): String {
    val host = config.property("host").getString()
    val port = config.property("port").getString()
    val name = config.property("name").getString()
    val username = config.property("username").getString()
    val password = config.property("password").getString()
    return "$connection:postgresql://$host:$port/$name?user=$username&password=$password"
}
