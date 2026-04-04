package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.ApplicationCall
import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.module.requestScope
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.PostgresIngredientStore
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess
import se.gustavkarlsson.chefgpt.requireSession

fun createIngredientStoreModule(config: ApplicationConfig) =
    module {
        requestScope {
            scoped {
                val db = get<PostgresAccess>()
                val call = get<ApplicationCall>()
                val userId = call.requireSession().user.id
                PostgresIngredientStore(db, userId)
            } bind IngredientStore::class
        }
    }
