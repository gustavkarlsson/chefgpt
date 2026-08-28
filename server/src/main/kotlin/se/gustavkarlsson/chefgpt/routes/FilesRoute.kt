package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.FILE_NAME_HEADER
import se.gustavkarlsson.chefgpt.files.FileUploader
import se.gustavkarlsson.chefgpt.files.attachmentKindOrNull

fun Route.filesRoute() {
    post("/files") {
        val contentType = call.request.contentType()
        if (attachmentKindOrNull(contentType) == null) {
            call.respond(
                HttpStatusCode.UnsupportedMediaType,
                ApiError(
                    type = "unsupported-file-type",
                    message = "Unsupported content type: $contentType",
                    userMessage = "I can only read images, PDFs and text files.",
                ),
            )
            return@post
        }
        val fileUploader = get<FileUploader>()
        val fileName = call.request.headers[FILE_NAME_HEADER]
        val attachment = fileUploader.uploadFile(call.receive(), contentType, fileName)
        if (attachment != null) {
            call.respond(HttpStatusCode.Created, attachment)
        } else {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
