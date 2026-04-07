package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.util.getOrFail
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.requireSession

fun Route.deleteIngredientRoute() {
    delete("/ingredients/{name}") {
        val ingredientStore = get<IngredientStore>()
        val userId = call.requireSession().user.id
        val name = call.parameters.getOrFail("name")
        val removed = ingredientStore.removeIngredients(userId, listOf(name))
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
