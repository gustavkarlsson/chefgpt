package se.gustavkarlsson.chefgpt.routes

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.agent.RecipeScanAgent
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiScanRecipe
import se.gustavkarlsson.chefgpt.requireSession

private val logger = LoggerFactory.getLogger("ScanRecipesRoute")

fun Route.scanRecipesRoute() {
    post("/recipes/scan") {
        val userId = call.requireSession().user.id
        val request = call.receive<ApiScanRecipe>()
        if (request.attachments.isEmpty()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(
                    type = "no-photos",
                    message = "No attachments",
                    userMessage = "Pick at least one photo to scan.",
                ),
            )
            return@post
        }
        if (request.attachments.any { !it.isImage }) {
            val offending = request.attachments.first { !it.isImage }.mimeType
            call.respond(
                HttpStatusCode.UnsupportedMediaType,
                ApiError(
                    type = "unsupported-file-type",
                    message = "Unsupported attachment: $offending",
                    userMessage = "I can only scan photos.",
                ),
            )
            return@post
        }

        // Block until the specialized agent has scanned the images.
        get<RecipeScanAgent>()
            .scan(userId, request.attachments)
            .onOk { summaries -> call.respond(HttpStatusCode.OK, summaries) }
            .onErr { reason ->
                // The failure reason is for us only; the user just sees a 500.
                logger.error("Recipe scan failed: {}", reason)
                call.respond(HttpStatusCode.InternalServerError)
            }
    }
}
