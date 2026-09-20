package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.agent.IngredientScanAgent
import se.gustavkarlsson.chefgpt.agent.toDomain
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.files.FileKind
import se.gustavkarlsson.chefgpt.files.FileUploader
import se.gustavkarlsson.chefgpt.files.fileKindOrNull
import se.gustavkarlsson.chefgpt.jobs.AgentJobScope
import se.gustavkarlsson.chefgpt.jobs.JobRepository
import se.gustavkarlsson.chefgpt.requireSession

private val logger = LoggerFactory.getLogger("ScanIngredientsRoute")

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
        val scanAgent = get<IngredientScanAgent>()
        val jobRepository = get<JobRepository>()

        val file = fileUploader.uploadFile(call.receive(), contentType)
        if (file == null) {
            call.respond(HttpStatusCode.InternalServerError)
            return@post
        }

        val job = jobRepository.create()
        get<AgentJobScope>().launch {
            try {
                val added = scanAgent.scan(userId, listOf(file.toDomain()))
                jobRepository.succeed(job.id, JsonArray(added.map { JsonPrimitive(it) }))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Ingredient scan failed", e)
                jobRepository.fail(job.id, ApiError("agent-failed", e.message ?: "Agent failed", userMessage = null))
            }
        }
        call.respond(HttpStatusCode.Accepted, job)
    }
}
