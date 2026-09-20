package se.gustavkarlsson.chefgpt.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.send
import kotlinx.coroutines.flow.collectLatest
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.util.sse

fun Route.streamRecipesRoute() {
    sse("/recipes") {
        val recipeRepository = get<RecipeRepository>()
        val userId = call.requireSession().user.id

        recipeRepository
            .streamRecipeSummaries(userId)
            .collectLatest { recipeSummaries ->
                send(recipeSummaries, "recipes")
            }
    }
}
