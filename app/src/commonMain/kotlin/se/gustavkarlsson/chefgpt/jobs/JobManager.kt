package se.gustavkarlsson.chefgpt.jobs

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The kinds of long-running work [JobManager] hosts. */
enum class JobType {
    ScanPhotos,
    ScrapeRecipe,
}

/**
 * Hosts app-lifetime work that must outlive any single screen. Each job runs
 * independently, and several jobs of the same [JobType] may run at once. Screens
 * observe [isRunning] to react to a job's progress.
 */
class JobManager(
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("JobManager")),
) {
    private val runningCounts = MutableStateFlow<Map<JobType, Int>>(emptyMap())

    fun run(
        type: JobType,
        block: suspend () -> Unit,
    ): Job {
        runningCounts.update { it + (type to ((it[type] ?: 0) + 1)) }
        return scope.launch {
            try {
                block()
            } finally {
                runningCounts.update { counts ->
                    val remaining = (counts[type] ?: 0) - 1
                    if (remaining <= 0) {
                        counts - type
                    } else {
                        counts + (type to remaining)
                    }
                }
            }
        }
    }

    /** True while at least one job of [type] is running. */
    fun isRunning(type: JobType): Flow<Boolean> = runningCounts.map { (it[type] ?: 0) > 0 }
}
