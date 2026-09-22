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
import kotlinx.io.files.Path
import kotlinx.serialization.builtins.ListSerializer
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.isImageFile
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.snackbar.ShowSnackbar

private val log = Logger.withTag("${HttpScanRecipes::class.simpleName}")

fun interface ScanRecipes {
    operator fun invoke(
        sessionId: SessionId,
        photos: List<Path>,
    )
}

/**
 * Scans photos into recipes as a [JobManager] job. The upload + scan + poll pipeline
 * runs in the manager's scope, so it finishes even after the screen that started it
 * goes away, and reports its outcome through the snackbar use case.
 */
class HttpScanRecipes(
    private val jobManager: JobManager,
    private val client: ChefGptClient,
    private val awaitJob: AwaitJob,
    private val showSnackbar: ShowSnackbar,
) : ScanRecipes {
    override operator fun invoke(
        sessionId: SessionId,
        photos: List<Path>,
    ) {
        // The picker offers documents too, but the scanner only reads photos.
        val images = photos.filter { isImageFile(it.name) }
        if (images.isEmpty()) {
            showSnackbar("That's not a photo I can scan", isError = true)
            return
        }
        jobManager.run(JobType.ScanPhotos) {
            val result =
                awaitJob(sessionId, ListSerializer(RecipeId.serializer())) {
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
                        showSnackbar("Couldn't find a recipe in those photos", isError = false)
                    } else {
                        showSnackbar("Saved $saved recipe(s)", isError = false)
                    }
                }.onErr { error ->
                    log.e { "Failed to scan recipes: $error" }
                    showSnackbar("Couldn't scan recipes from the photos", isError = true)
                }
        }
    }
}
