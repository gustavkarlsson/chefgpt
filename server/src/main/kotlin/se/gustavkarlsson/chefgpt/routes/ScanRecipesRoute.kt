package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
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
import se.gustavkarlsson.chefgpt.agent.RecipeScanAgent
import se.gustavkarlsson.chefgpt.agent.toDomain
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiScanRecipe
import se.gustavkarlsson.chefgpt.jobs.AgentJobScope
import se.gustavkarlsson.chefgpt.jobs.JobRepository
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

        val scanAgent = get<RecipeScanAgent>()
        val jobRepository = get<JobRepository>()
        val job = jobRepository.create()
        get<AgentJobScope>().launch {
            try {
                val added = scanAgent.scan(userId, sharedFiles)
                jobRepository.succeed(job.id, JsonArray(added.map { JsonPrimitive(it) }))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Recipe scan failed", e)
                jobRepository.fail(job.id, ApiError("agent-failed", e.message ?: "Agent failed", userMessage = null))
            }
        }
        call.respond(HttpStatusCode.Accepted, job)
    }
}
