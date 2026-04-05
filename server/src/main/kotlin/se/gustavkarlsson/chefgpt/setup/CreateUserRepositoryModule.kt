package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.PostgresUserRepository
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.auth.registrationRules
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess

fun Application.createUserRepositoryModule() =
    module {
        single {
            val database = getOrNull<PostgresAccess>()
            if (database != null) {
                PostgresUserRepository(database, registrationRules)
            } else {
                InMemoryUserRepository(registrationRules)
            }
        } bind UserRepository::class
    }
