package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.util.getOrFail
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.requireSession

fun Route.deleteIngredientRoute() {
    delete("/ingredients/{id}") {
        val ingredientStore = get<IngredientStore>()
        val userId = call.requireSession().user.id
        val id =
            IngredientId.parseOrNull(call.parameters.getOrFail("id"))
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("invalid-ingredient-id", "Invalid ingredient id", userMessage = null),
                )
        val destroyed = ingredientStore.destroyIngredients(userId, listOf(id))
        if (destroyed.isNotEmpty()) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(
                HttpStatusCode.NotFound,
                ApiError("ingredient-not-found", "Ingredient not found", userMessage = null),
            )
        }
    }
}
