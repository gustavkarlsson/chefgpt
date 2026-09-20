package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.util.getOrFail
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.JobId
import se.gustavkarlsson.chefgpt.jobs.JobRepository

fun Route.getJobRoute() {
    get("/jobs/{jobId}") {
        val jobId =
            JobId.parseOrNull(call.parameters.getOrFail("jobId"))
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("invalid-job-id", "Invalid job id", userMessage = null),
                )

        val job =
            get<JobRepository>()[jobId]
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ApiError("job-not-found", "Job not found", userMessage = null),
                )

        call.respond(HttpStatusCode.OK, job)
    }
}
