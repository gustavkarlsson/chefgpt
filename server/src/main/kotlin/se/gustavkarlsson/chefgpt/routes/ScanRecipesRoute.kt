package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.agent.saverecipes.SaveRecipesAgent
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiScanRecipe
import se.gustavkarlsson.chefgpt.jobs.JobRunner
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.toDomain

fun Route.scanRecipesRoute() {
    post("/recipes/scan") {
        val userId = call.requireSession().user.id
        val request = call.receive<ApiScanRecipe>()
        if (request.files.isEmpty()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(
                    type = "no-photos",
                    message = "No files",
                    userMessage = "Pick at least one photo to scan.",
                ),
            )
            return@post
        }
        if (request.files.any { !it.isImage }) {
            val offending = request.files.first { !it.isImage }.mimeType
            call.respond(
                HttpStatusCode.UnsupportedMediaType,
                ApiError(
                    type = "unsupported-file-type",
                    message = "Unsupported file: $offending",
                    userMessage = "I can only scan photos.",
                ),
            )
            return@post
        }

        val sharedFiles = request.files.map { it.toDomain() }
        if (sharedFiles.isEmpty()) {
            call.respond(HttpStatusCode.NoContent)
            return@post
        }

        val scanAgent = get<SaveRecipesAgent>()
        val job =
            get<JobRunner>().run("Recipe scan", ListSerializer(String.serializer())) {
                scanAgent.scan(userId, sharedFiles)
            }
        call.respond(HttpStatusCode.Accepted, job)
    }
}
