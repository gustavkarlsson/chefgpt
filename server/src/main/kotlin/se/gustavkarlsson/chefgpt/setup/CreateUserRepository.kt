package se.gustavkarlsson.chefgpt.setup

import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.PostgresUserRepository
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.auth.registrationRules
import se.gustavkarlsson.chefgpt.db.DatabaseAccess

fun createUserRepository(database: DatabaseAccess?): UserRepository =
    if (database != null) {
        PostgresUserRepository(database, registrationRules)
    } else {
        InMemoryUserRepository(registrationRules)
    }
