package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.requireSession

fun Route.getIngredientsRoute() {
    get("/ingredients") {
        val ingredientStore = get<IngredientStore>()
        val userId = call.requireSession().user.id
        val ingredients = ingredientStore.getIngredients(userId)
        call.respond(HttpStatusCode.OK, ingredients)
    }
}
