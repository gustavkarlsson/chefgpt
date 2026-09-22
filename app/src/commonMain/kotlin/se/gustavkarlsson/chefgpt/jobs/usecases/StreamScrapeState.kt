package se.gustavkarlsson.chefgpt.jobs.usecases

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.jobs.JobManager
import se.gustavkarlsson.chefgpt.jobs.JobType

fun interface StreamScrapeState {
    operator fun invoke(): Flow<Boolean>
}

class RealStreamScrapeState(
    private val jobManager: JobManager,
) : StreamScrapeState {
    override operator fun invoke(): Flow<Boolean> = jobManager.isRunning(JobType.ScrapeRecipe)
}
