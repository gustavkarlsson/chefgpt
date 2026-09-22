package se.gustavkarlsson.chefgpt.screens.recipescrape

import kotlinx.coroutines.flow.update
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.jobs.usecases.ScrapeRecipe
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.StateViewModel
import se.gustavkarlsson.chefgpt.sessions.SessionId

class RecipeScrapeSheetViewModel(
    private val navigator: Navigator,
    private val scrapeRecipe: ScrapeRecipe,
    @InjectedParam screen: RecipeScrapeSheet,
) : StateViewModel<RecipeScrapeSheetState, RecipeScrapeSheetUiState>() {
    private val sessionId: SessionId = screen.sessionId

    override fun createInitialState() = RecipeScrapeSheetState(url = "")

    override fun RecipeScrapeSheetState.toUiState(): RecipeScrapeSheetUiState =
        RecipeScrapeSheetUiState(
            url = url,
            onUrlChange = ::updateUrl,
            onClickConfirm = if (url.isBlank()) null else ::confirm,
        )

    private fun updateUrl(url: String) {
        innerState.update { it.copy(url = url) }
    }

    private fun confirm() {
        scrapeRecipe(sessionId, innerState.value.url.trim())
        navigator.pop()
    }
}

data class RecipeScrapeSheetState(
    val url: String,
)

data class RecipeScrapeSheetUiState(
    val url: String,
    val onUrlChange: (String) -> Unit,
    val onClickConfirm: (() -> Unit)?,
)
