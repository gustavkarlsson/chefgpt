package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.images.ImageUploader

fun Route.imagesRoute() {
    post("/images") {
        val imageUploader = get<ImageUploader>()
        val contentType = call.request.contentType()
        val imageUrl = imageUploader.uploadImage(call.receive(), contentType)
        if (imageUrl != null) {
            call.respond(HttpStatusCode.Created, imageUrl.value)
        } else {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
