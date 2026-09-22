package se.gustavkarlsson.chefgpt.jobs.usecases

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.serialization.builtins.ListSerializer
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.jobs.JobManager
import se.gustavkarlsson.chefgpt.jobs.JobType
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar

private val log = Logger.withTag("${HttpScrapeRecipe::class.simpleName}")

fun interface ScrapeRecipe {
    operator fun invoke(
        sessionId: SessionId,
        url: String,
    )
}

/**
 * Scrapes a recipe from a website URL as a [JobManager] job. The scrape + poll pipeline
 * runs in the manager's scope, so it finishes even after the screen that started it
 * goes away, and reports its outcome through the snackbar use case.
 */
class HttpScrapeRecipe(
    private val jobManager: JobManager,
    private val client: ChefGptClient,
    private val awaitJob: AwaitJob,
    private val showSnackbar: ShowSnackbar,
) : ScrapeRecipe {
    override operator fun invoke(
        sessionId: SessionId,
        url: String,
    ) {
        jobManager.run(JobType.ScrapeRecipe) {
            val result =
                awaitJob(sessionId, ListSerializer(RecipeId.serializer())) {
                    client.scrapeRecipe(sessionId, url)
                }
            result
                .onOk { job ->
                    if (job.result.orEmpty().isEmpty()) {
                        showSnackbar("Couldn't extract a recipe from that page", isError = false)
                    } else {
                        showSnackbar("Saved recipe", isError = false)
                    }
                }.onErr { error ->
                    log.e { "Failed to scrape recipe: $error" }
                    showSnackbar("Couldn't scrape a recipe from the website", isError = true)
                }
        }
    }
}
