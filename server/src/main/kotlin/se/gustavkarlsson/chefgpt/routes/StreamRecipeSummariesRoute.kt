package se.gustavkarlsson.chefgpt.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.send
import kotlinx.coroutines.flow.collectLatest
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.recipes.RecipeSummaryStore
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.util.sse

fun Route.streamRecipeSummariesRoute() {
    sse("/recipe-summaries") {
        val recipeSummaryStore = get<RecipeSummaryStore>()
        val userId = call.requireSession().user.id

        recipeSummaryStore
            .streamRecipeSummaries(userId)
            .collectLatest { recipeSummaries ->
                send(recipeSummaries, "recipe-summaries")
            }
    }
}
