package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.errors.v1.ApiError
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiSaveSpoonacularRecipe
import se.gustavkarlsson.chefgpt.api.recipes.v1.RECIPES_V1_PATH
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.Spoonacular
import se.gustavkarlsson.chefgpt.recipes.toApi
import se.gustavkarlsson.chefgpt.requireSession

fun Route.saveRecipeRoute() {
    post(RECIPES_V1_PATH) {
        val recipeRepository = get<RecipeRepository>()
        val spoonacular = get<Spoonacular>()
        val userId = call.requireSession().user.id
        val spoonacularId = call.receive<ApiSaveSpoonacularRecipe>().spoonacularId

        val lookedUp =
            spoonacular.lookUp(spoonacularId)
                ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    ApiError("recipe-not-found", "Recipe not found", userMessage = null),
                )

        val saved = recipeRepository.saveRecipe(userId, lookedUp)
        call.respond(HttpStatusCode.Created, saved.toApi())
    }
}
