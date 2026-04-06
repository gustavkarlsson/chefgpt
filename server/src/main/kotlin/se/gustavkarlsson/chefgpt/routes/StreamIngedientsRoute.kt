package se.gustavkarlsson.chefgpt.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.send
import io.ktor.server.sse.sse
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.scope
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import kotlin.time.Duration.Companion.seconds

fun Route.streamIngredientsRoute() {
    sse(
        "/ingredients",
        serialize = { typeInfo, value ->
            val json = get<Json>()
            val serializer = json.serializersModule.serializer(typeInfo.kotlinType!!)
            json.encodeToString(serializer, value)
        },
    ) {
        heartbeat {
            period = 15.seconds
        }
        val ingredientStore = call.scope.get<IngredientStore>()

        ingredientStore
            .streamIngredients()
            .collectLatest { ingredients ->
                send(ingredients)
            }
    }
}
