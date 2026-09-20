package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.recipes.InMemoryRecipePersistence
import se.gustavkarlsson.chefgpt.recipes.PostgresRecipePersistence
import se.gustavkarlsson.chefgpt.recipes.RecipePersistence
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository

fun Application.createRecipeRepositoryModule() =
    module {
        single<RecipePersistence> {
            val db = getOrNull<DatabaseAccess>()
            if (db != null) {
                PostgresRecipePersistence(db)
            } else {
                InMemoryRecipePersistence()
            }
        }
        single {
            RecipeRepository(get<RecipePersistence>())
        }
    }
