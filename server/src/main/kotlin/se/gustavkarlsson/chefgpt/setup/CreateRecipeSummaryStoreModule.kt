package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.recipes.InMemoryRecipeSummaryStore
import se.gustavkarlsson.chefgpt.recipes.PostgresRecipeSummaryStore
import se.gustavkarlsson.chefgpt.recipes.RecipeSummaryStore

fun Application.createRecipeSummaryStoreModule() =
    module {
        single {
            val db = getOrNull<DatabaseAccess>()
            if (db != null) {
                PostgresRecipeSummaryStore(db)
            } else {
                InMemoryRecipeSummaryStore()
            }
        } bind RecipeSummaryStore::class
    }
