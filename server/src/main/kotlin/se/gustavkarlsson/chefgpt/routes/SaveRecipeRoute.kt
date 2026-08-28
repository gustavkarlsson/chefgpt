package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiSaveSpoonacularRecipe
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeStore
import se.gustavkarlsson.chefgpt.requireSession

fun Route.saveRecipeRoute() {
    post("/recipes") {
        val recipeStore = get<RecipeStore>()
        val recipeLookup = get<RecipeLookup>()
        val userId = call.requireSession().user.id
        val spoonacularId = call.receive<ApiSaveSpoonacularRecipe>().spoonacularId

        val lookedUp =
            recipeLookup.lookUp(spoonacularId)
                ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    ApiError("recipe-not-found", "Recipe not found"),
                )

        val saved = recipeStore.saveRecipe(userId, lookedUp)
        call.respond(HttpStatusCode.Created, saved)
    }
}
