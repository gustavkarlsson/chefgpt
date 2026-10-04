package se.gustavkarlsson.chefgpt.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.send
import kotlinx.coroutines.flow.collectLatest
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ingredients.v1.INGREDIENTS_V1_PATH
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.toApi
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.util.sse

// TODO Add tests (Not snapshot test, as they are not possible)
fun Route.streamIngredientsRoute() {
    sse(INGREDIENTS_V1_PATH) {
        val ingredientStore = get<IngredientStore>()
        val userId = call.requireSession().user.id

        ingredientStore
            .streamIngredients(userId)
            .collectLatest { ingredients ->
                val apiIngredients = ingredients.map { it.toApi() }
                send(apiIngredients, "ingredients")
            }
    }
}
