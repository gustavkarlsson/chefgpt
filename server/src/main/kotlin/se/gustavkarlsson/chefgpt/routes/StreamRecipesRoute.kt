package se.gustavkarlsson.chefgpt.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.send
import kotlinx.coroutines.flow.collectLatest
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.recipes.v1.RECIPES_V1_PATH
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.toApi
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.util.sse

fun Route.streamRecipesRoute() {
    val recipeRepository = get<RecipeRepository>()
    sse(RECIPES_V1_PATH) {
        val userId = call.requireSession().user.id

        recipeRepository
            .streamRecipeSummaries(userId)
            .collectLatest { recipeSummaries ->
                val apiSummaries = recipeSummaries.map { it.toApi() }
                send(apiSummaries, "recipes")
            }
    }
}
