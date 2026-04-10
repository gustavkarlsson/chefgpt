package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import io.ktor.server.util.getOrFail
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.requireSession

fun Route.putIngredientRoute() {
    put("/ingredients/{name}") {
        val ingredientStore = get<IngredientStore>()
        val userId = call.requireSession().user.id
        val name = call.parameters.getOrFail("name")
        val added = ingredientStore.addIngredients(userId, listOf(name))
        if (added.isNotEmpty()) {
            call.respond(HttpStatusCode.Created)
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
