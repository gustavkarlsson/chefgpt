package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.util.getOrFail
import org.koin.ktor.plugin.scope
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

fun Route.deleteIngredientRoute() {
    delete("/ingredients/{name}") {
        val name = call.parameters.getOrFail("name")
        val ingredientStore = call.scope.get<IngredientStore>()
        val removed = ingredientStore.removeIngredients(listOf(name))
        if (removed.isNotEmpty()) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(
                HttpStatusCode.NotFound,
                ApiError("ingredient-not-found", "Ingredient not found"),
            )
        }
    }
}
