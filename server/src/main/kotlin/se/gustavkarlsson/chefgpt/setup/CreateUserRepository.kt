package se.gustavkarlsson.chefgpt.setup

import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.PostgresUserRepository
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.auth.registrationRules
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase

fun createUserRepository(database: ChefGptDatabase?): UserRepository =
    if (database != null) {
        PostgresUserRepository(database.userQueries, registrationRules)
    } else {
        InMemoryUserRepository(registrationRules)
    }
