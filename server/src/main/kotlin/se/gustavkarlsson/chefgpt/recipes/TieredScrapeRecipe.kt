package se.gustavkarlsson.chefgpt.recipes

import org.slf4j.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a chain of scrapers in sequence, handing each the best result the previous one managed.
 * Stops at the first complete recipe, and rethrows the first scraper's failure when none completes.
 */
class TieredScrapeRecipe(
    private val scrapers: List<RecipeScraper>,
) : RecipeScraper {
    override suspend fun scrape(
        url: String,
        partialRecipe: NewRecipe?,
    ): NewRecipe? {
        if (partialRecipe?.isComplete() == true) {
            return partialRecipe
        }
        var latestPartial = partialRecipe
        var firstException: Exception? = null
        for (scraper in scrapers) {
            val name = scraper::class.simpleName
            val result =
                try {
                    scraper.scrape(url, latestPartial)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.warn("$name failed to scrape $url: ${e.message}")
                    firstException = firstException ?: e
                    continue
                }
            when {
                result == null -> {
                    logger.info("$name found no recipe at $url")
                }

                result.isComplete() -> {
                    return result
                }

                else -> {
                    latestPartial = result
                }
            }
        }
        firstException?.let { throw it }
        return null
    }
}

private fun NewRecipe.isComplete(): Boolean = title.isNotBlank() && steps.isNotEmpty() && ingredients.isNotEmpty()

private val logger = LoggerFactory.getLogger("TieredScrapeRecipe")
