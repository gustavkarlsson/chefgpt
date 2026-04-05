package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import io.ktor.server.util.getOrFail
import org.koin.ktor.plugin.scope
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

fun Route.putIngredientRoute() {
    put("/ingredients/{name}") {
        val name = call.parameters.getOrFail("name")
        val ingredientStore = call.scope.get<IngredientStore>()
        val added = ingredientStore.addIngredients(listOf(name))
        if (added.isNotEmpty()) {
            call.respond(HttpStatusCode.Created)
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
