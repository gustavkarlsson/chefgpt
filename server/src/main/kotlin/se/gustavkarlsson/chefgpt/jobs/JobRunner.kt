package se.gustavkarlsson.chefgpt.jobs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiJob

/**
 * Runs work in the background and tracks it as a job the client can poll. The work's result is
 * serialized to JSON when it succeeds, so the job stores JsonElement and one job endpoint serves
 * jobs of every result type.
 */
class JobRunner(
    private val jobRepository: JobRepository,
    private val scope: CoroutineScope,
    private val json: Json,
) {
    suspend fun <T> run(
        name: String,
        serializer: KSerializer<T>,
        work: suspend () -> T,
    ): ApiJob<JsonElement> {
        val job = jobRepository.create()
        scope.launch {
            try {
                jobRepository.succeed(job.id, json.encodeToJsonElement(serializer, work()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("$name failed", e)
                jobRepository.fail(job.id, ApiError("agent-failed", e.message ?: "Agent failed", userMessage = null))
            }
        }
        return job
    }
}

private val logger = LoggerFactory.getLogger("JobRunner")
