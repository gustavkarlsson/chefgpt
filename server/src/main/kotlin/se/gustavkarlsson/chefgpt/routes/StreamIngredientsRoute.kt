package se.gustavkarlsson.chefgpt.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.send
import kotlinx.coroutines.flow.collectLatest
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.util.sse

// TODO Add tests (Not snapshot test, as they are not possible)
fun Route.streamIngredientsRoute() {
    sse("/ingredients") {
        val ingredientStore = get<IngredientStore>()
        val userId = call.requireSession().user.id

        ingredientStore
            .streamIngredients(userId)
            .collectLatest { ingredients ->
                send(ingredients, "ingredients")
            }
    }
}
