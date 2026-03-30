package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import se.gustavkarlsson.chefgpt.rethinkdb.RethinkDbAccess

fun createRethinkDbAccessOrNull(config: ApplicationConfig): RethinkDbAccess? =
    when (val storage = config.property("chefgpt.storage").getString()) {
        "database" -> {
            val rethinkConfig = config.config("rethinkdb")
            val access =
                RethinkDbAccess(
                    host = rethinkConfig.property("host").getString(),
                    port = rethinkConfig.property("port").getString().toInt(),
                    database = rethinkConfig.property("database").getString(),
                    username = rethinkConfig.propertyOrNull("username")?.getString(),
                    password = rethinkConfig.propertyOrNull("password")?.getString(),
                )
            access.initialize()
            access
        }

        "memory" -> {
            null
        }

        else -> {
            error("chefgpt.storage must be 'memory' or 'database', got '$storage'")
        }
    }
