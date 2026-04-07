package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.PostgresUserRepository
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.auth.registrationRules
import se.gustavkarlsson.chefgpt.postgres.PostgresDatabasePool

fun Application.createUserRepositoryModule() =
    module {
        single {
            val dbPool = getOrNull<PostgresDatabasePool>()
            if (dbPool != null) {
                PostgresUserRepository(dbPool, registrationRules)
            } else {
                InMemoryUserRepository(registrationRules)
            }
        } bind UserRepository::class
    }
