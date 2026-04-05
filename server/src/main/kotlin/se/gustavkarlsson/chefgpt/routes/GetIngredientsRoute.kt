package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.plugin.scope
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

fun Route.getIngredientsRoute() {
    get("/ingredients") {
        val ingredientStore = call.scope.get<IngredientStore>()
        val ingredients = ingredientStore.getIngredients()
        call.respond(HttpStatusCode.OK, ingredients)
    }
}
