package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.facts.InMemoryFactRepository
import se.gustavkarlsson.chefgpt.facts.PostgresFactRepository
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess

fun Application.createFactRepositoryModule() =
    module {
        single {
            val db = getOrNull<DatabaseAccess>()
            if (db != null) {
                PostgresFactRepository(db, get())
            } else {
                InMemoryFactRepository()
            }
        } bind FactRepository::class
    }
