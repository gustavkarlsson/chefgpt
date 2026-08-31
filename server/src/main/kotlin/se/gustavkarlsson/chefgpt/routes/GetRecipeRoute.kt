package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.util.getOrFail
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.recipes.RecipeStore
import se.gustavkarlsson.chefgpt.requireSession

fun Route.getRecipeRoute() {
    get("/recipes/{id}") {
        val recipeStore = get<RecipeStore>()
        val userId = call.requireSession().user.id
        val id =
            RecipeId.parseOrNull(call.parameters.getOrFail("id"))
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("invalid-recipe-id", "Invalid recipe id", userMessage = null),
                )

        val recipe =
            recipeStore.getRecipe(userId, id)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ApiError("recipe-not-found", "Recipe not found", userMessage = null),
                )

        call.respond(HttpStatusCode.OK, recipe)
    }
}
