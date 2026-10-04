package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.util.getOrFail
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.common.RecipeId
import se.gustavkarlsson.chefgpt.api.errors.v1.ApiError
import se.gustavkarlsson.chefgpt.api.recipes.v1.RECIPES_V1_PATH
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.requireSession

fun Route.deleteRecipeRoute() {
    delete("$RECIPES_V1_PATH/{id}") {
        val recipeRepository = get<RecipeRepository>()
        val userId = call.requireSession().user.id
        val id =
            RecipeId.parseOrNull(call.parameters.getOrFail("id"))
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("invalid-recipe-id", "Invalid recipe id", userMessage = null),
                )
        if (recipeRepository.deleteRecipe(userId, id)) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(
                HttpStatusCode.NotFound,
                ApiError("recipe-not-found", "Recipe not found", userMessage = null),
            )
        }
    }
}
