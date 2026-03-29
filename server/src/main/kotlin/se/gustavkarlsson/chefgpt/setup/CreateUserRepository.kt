package se.gustavkarlsson.chefgpt.setup

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.PostgresUserRepository
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.auth.registrationRules

fun createUserRepository(database: R2dbcDatabase?): UserRepository =
    if (database != null) {
        PostgresUserRepository(database, registrationRules)
    } else {
        InMemoryUserRepository(registrationRules)
    }
