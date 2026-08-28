package se.gustavkarlsson.chefgpt.routes

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.agent.IngredientScanAgent
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.files.AttachmentKind
import se.gustavkarlsson.chefgpt.files.FileUploader
import se.gustavkarlsson.chefgpt.files.attachmentKindOrNull
import se.gustavkarlsson.chefgpt.requireSession

private val logger = LoggerFactory.getLogger("ScanIngredientsRoute")

fun Route.scanIngredientsRoute() {
    post("/ingredients/scan") {
        val userId = call.requireSession().user.id
        val contentType = call.request.contentType()
        if (attachmentKindOrNull(contentType) != AttachmentKind.Image) {
            call.respond(
                HttpStatusCode.UnsupportedMediaType,
                ApiError(
                    type = "unsupported-file-type",
                    message = "Unsupported content type: $contentType",
                    userMessage = "I can only scan photos.",
                ),
            )
            return@post
        }
        val fileUploader = get<FileUploader>()
        val scanAgent = get<IngredientScanAgent>()

        val attachment = fileUploader.uploadFile(call.receive(), contentType)
        if (attachment == null) {
            call.respond(HttpStatusCode.InternalServerError)
            return@post
        }

        // Block until the specialized agent has scanned the image.
        with(scanAgent) { scan(userId, attachment) }
            .onOk { count -> call.respond(HttpStatusCode.OK, count.toString()) }
            .onErr { reason ->
                // The failure reason is for us only; the user just sees a 500.
                logger.error("Ingredient scan failed: {}", reason)
                call.respond(HttpStatusCode.InternalServerError)
            }
    }
}
