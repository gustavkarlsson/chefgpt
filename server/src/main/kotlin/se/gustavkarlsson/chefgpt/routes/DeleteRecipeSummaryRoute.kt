package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.util.getOrFail
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.RecipeSummaryId
import se.gustavkarlsson.chefgpt.recipes.RecipeSummaryStore
import se.gustavkarlsson.chefgpt.requireSession

fun Route.deleteRecipeSummaryRoute() {
    delete("/recipe-summaries/{id}") {
        val recipeSummaryStore = get<RecipeSummaryStore>()
        val userId = call.requireSession().user.id
        val id =
            RecipeSummaryId.parseOrNull(call.parameters.getOrFail("id"))
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("invalid-recipe-summary-id", "Invalid recipe summary id"),
                )
        if (recipeSummaryStore.deleteRecipeSummary(userId, id)) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(
                HttpStatusCode.NotFound,
                ApiError("recipe-summary-not-found", "Recipe summary not found"),
            )
        }
    }
}
