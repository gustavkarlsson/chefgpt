package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiNewIngredient
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.requireSession

fun Route.createIngredientRoute() {
    post("/ingredients") {
        val ingredientStore = get<IngredientStore>()
        val userId = call.requireSession().user.id
        val name = call.receive<ApiNewIngredient>().name
        val added = ingredientStore.createIngredients(userId, listOf(name)).singleOrNull()
        if (added != null) {
            call.respond(HttpStatusCode.Created, added)
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
