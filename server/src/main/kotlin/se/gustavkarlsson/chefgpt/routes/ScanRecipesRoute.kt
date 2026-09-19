package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.agent.RecipeScanAgent
import se.gustavkarlsson.chefgpt.agent.toDomain
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiScanRecipe
import se.gustavkarlsson.chefgpt.requireSession

private val logger = LoggerFactory.getLogger("ScanRecipesRoute")

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

        // Block until the specialized agent has scanned the images.
        val added = get<RecipeScanAgent>().scan(userId, sharedFiles)
        call.respond(HttpStatusCode.OK, added.joinToString())
    }
}
