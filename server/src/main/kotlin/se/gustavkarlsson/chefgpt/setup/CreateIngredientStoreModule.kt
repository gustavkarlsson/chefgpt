package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.module.requestScope
import se.gustavkarlsson.chefgpt.ingredients.InMemoryIngredientStoreFactory
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.PostgresIngredientStore
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess
import se.gustavkarlsson.chefgpt.requireSession

fun Application.createIngredientStoreModule() =
    module {
        single { InMemoryIngredientStoreFactory() }
        requestScope {
            scoped {
                val call = get<ApplicationCall>()
                val userId = call.requireSession().user.id
                val db = getOrNull<PostgresAccess>()
                if (db != null) {
                    PostgresIngredientStore(db, userId)
                } else {
                    get<InMemoryIngredientStoreFactory>().create(userId)
                }
            } bind IngredientStore::class
        }
    }
