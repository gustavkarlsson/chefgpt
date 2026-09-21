package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.agent.scaningredients.ScanIngredientsAgent
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.files.FileKind
import se.gustavkarlsson.chefgpt.files.FileUploader
import se.gustavkarlsson.chefgpt.files.fileKindOrNull
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.jobs.JobRunner
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.toDomain

fun Route.scanIngredientsRoute() {
    post("/ingredients/scan") {
        val userId = call.requireSession().user.id
        val contentType = call.request.contentType()
        if (fileKindOrNull(contentType) != FileKind.Image) {
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
        val scanAgent = get<ScanIngredientsAgent>()
        val ingredientStore = get<IngredientStore>()

        val file = fileUploader.uploadFile(call.receive(), contentType)
        if (file == null) {
            call.respond(HttpStatusCode.InternalServerError)
            return@post
        }

        val job =
            get<JobRunner>().run("Ingredient scan", ListSerializer(String.serializer())) {
                val scanned = scanAgent.scan(userId, listOf(file.toDomain()))
                val added = ingredientStore.createIngredients(userId, scanned)
                added.map { it.name }
            }
        call.respond(HttpStatusCode.Accepted, job)
    }
}
