package se.gustavkarlsson.chefgpt.jobs.usecases

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.jobs.JobManager
import se.gustavkarlsson.chefgpt.jobs.JobType

fun interface StreamScanState {
    operator fun invoke(): Flow<Boolean>
}

class RealStreamScanState(
    private val jobManager: JobManager,
) : StreamScanState {
    override operator fun invoke(): Flow<Boolean> = jobManager.isRunning(JobType.ScanPhotos)
}
