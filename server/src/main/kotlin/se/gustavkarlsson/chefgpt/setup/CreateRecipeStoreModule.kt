package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.recipes.InMemoryRecipeStore
import se.gustavkarlsson.chefgpt.recipes.PostgresRecipeStore
import se.gustavkarlsson.chefgpt.recipes.RecipeStore

fun Application.createRecipeStoreModule() =
    module {
        single {
            val db = getOrNull<DatabaseAccess>()
            if (db != null) {
                PostgresRecipeStore(db)
            } else {
                InMemoryRecipeStore()
            }
        } bind RecipeStore::class
    }
