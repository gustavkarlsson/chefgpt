package se.gustavkarlsson.chefgpt.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.send
import kotlinx.coroutines.flow.collectLatest
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.recipes.RecipeStore
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.util.sse

fun Route.streamRecipesRoute() {
    sse("/recipes") {
        val recipeStore = get<RecipeStore>()
        val userId = call.requireSession().user.id

        recipeStore
            .streamRecipes(userId)
            .collectLatest { recipes ->
                send(recipes, "recipes")
            }
    }
}
