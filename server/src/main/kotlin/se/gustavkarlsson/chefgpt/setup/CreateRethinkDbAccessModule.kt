package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.rethinkdb.RethinkDbAccess

fun createRethinkDbAccessModule(config: ApplicationConfig) =
    module {
        when (val storage = config.property("chefgpt.storage").getString()) {
            "database" -> {
                single {
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
            }

            "memory" -> {
                Unit
            }

            else -> {
                error("chefgpt.storage must be 'memory' or 'database', got '$storage'")
            }
        }
    }
