package se.gustavkarlsson.chefgpt.jobs

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.combine
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.http.ContentType
import io.ktor.http.defaultForFilePath
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.io.files.Path
import kotlinx.serialization.builtins.ListSerializer
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.isImageFile
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.snackbar.SnackbarManager

private val log = Logger.withTag("${ScanRecipes::class.simpleName}")

/**
 * Scans photos into recipes as a [JobManager] job. The upload + scan + poll pipeline
 * runs in the manager's scope, so it finishes even after the screen that started it
 * goes away, and reports its outcome through the snackbar manager.
 */
class ScanRecipes(
    private val jobManager: JobManager,
    private val client: ChefGptClient,
    private val awaitJob: AwaitJobUseCase,
    private val snackbarManager: SnackbarManager,
) {
    val isScanning: Flow<Boolean> = jobManager.isRunning(JobType.ScanPhotos)

    fun scan(
        sessionId: SessionId,
        photos: List<Path>,
    ) {
        // The picker offers documents too, but the scanner only reads photos.
        val images = photos.filter { isImageFile(it.name) }
        if (images.isEmpty()) {
            snackbarManager.show("That's not a photo I can scan", isError = true)
            return
        }
        jobManager.run(JobType.ScanPhotos) {
            val result =
                awaitJob.await(sessionId, ListSerializer(RecipeId.serializer())) {
                    coroutineScope {
                        images
                            .map { file ->
                                async {
                                    client.uploadFile(
                                        sessionId,
                                        file,
                                        ContentType.defaultForFilePath(file.name),
                                    )
                                }
                            }.awaitAll()
                            .combine()
                            .flatMap { attachments -> client.scanRecipes(sessionId, attachments) }
                    }
                }
            result
                .onOk { job ->
                    val saved = job.result.orEmpty().size
                    if (saved == 0) {
                        snackbarManager.show("Couldn't find a recipe in those photos")
                    } else {
                        snackbarManager.show("Saved $saved recipe(s)")
                    }
                }.onErr { error ->
                    log.e { "Failed to scan recipes: $error" }
                    snackbarManager.show("Couldn't scan recipes from the photos", isError = true)
                }
        }
    }
}
