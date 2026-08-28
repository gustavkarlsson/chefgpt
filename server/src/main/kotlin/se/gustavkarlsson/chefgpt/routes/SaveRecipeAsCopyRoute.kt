package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.util.getOrFail
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.recipes.RecipeStore
import se.gustavkarlsson.chefgpt.requireSession

fun Route.saveRecipeAsCopyRoute() {
    post("/recipes/{id}/save-as-copy") {
        val recipeStore = get<RecipeStore>()
        val userId = call.requireSession().user.id
        val id =
            RecipeId.parseOrNull(call.parameters.getOrFail("id"))
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("invalid-recipe-id", "Invalid recipe id"),
                )
        val recipe =
            recipeStore.getRecipe(userId, id)
                ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    ApiError("recipe-not-found", "Recipe not found"),
                )
        if (recipe.modifiedFrom == null) {
            return@post call.respond(
                HttpStatusCode.Conflict,
                ApiError("recipe-not-modified", "Recipe is not a modified version of another recipe"),
            )
        }

        val copy =
            recipeStore.saveAsCopy(userId, id)
                ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    ApiError("recipe-not-found", "Recipe not found"),
                )
        call.respond(HttpStatusCode.OK, copy)
    }
}
