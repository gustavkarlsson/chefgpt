package se.gustavkarlsson.chefgpt.jobs

import kotlinx.coroutines.flow.Flow

fun interface StreamScanState {
    operator fun invoke(): Flow<Boolean>
}

class RealStreamScanState(
    private val jobManager: JobManager,
) : StreamScanState {
    override operator fun invoke(): Flow<Boolean> = jobManager.isRunning(JobType.ScanPhotos)
}
