package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.builtins.ListSerializer
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiScrapeRecipe
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.jobs.JobRunner
import se.gustavkarlsson.chefgpt.recipes.SaveRecipeFromUrl
import se.gustavkarlsson.chefgpt.requireSession

fun Route.scrapeRecipesRoute() {
    post("/recipes/scrape") {
        val userId = call.requireSession().user.id
        val url = call.receive<ApiScrapeRecipe>().url
        if (url.isBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(
                    type = "no-url",
                    message = "No URL",
                    userMessage = "Paste a website URL to scrape.",
                ),
            )
            return@post
        }

        val saveRecipeFromUrl = get<SaveRecipeFromUrl>()
        val job =
            get<JobRunner>().run("Recipe scrape", ListSerializer(RecipeId.serializer())) {
                saveRecipeFromUrl.save(userId, url)?.let { listOf(it.id) } ?: emptyList()
            }
        call.respond(HttpStatusCode.Accepted, job)
    }
}
