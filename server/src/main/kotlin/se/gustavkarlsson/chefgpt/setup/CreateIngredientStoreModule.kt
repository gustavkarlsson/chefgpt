package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.ingredients.InMemoryIngredientStore
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.PostgresIngredientStore
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess

fun Application.createIngredientStoreModule() =
    module {
        single {
            val db = getOrNull<DatabaseAccess>()
            if (db != null) {
                PostgresIngredientStore(db)
            } else {
                InMemoryIngredientStore()
            }
        } bind IngredientStore::class
    }
