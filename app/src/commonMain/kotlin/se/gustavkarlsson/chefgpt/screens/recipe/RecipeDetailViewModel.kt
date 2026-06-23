package se.gustavkarlsson.chefgpt.screens.recipe

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.recipes.Recipe
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.screens.StateViewModel

private val log = Logger.withTag("${RecipeDetailViewModel::class.simpleName}")

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val navigator: Navigator,
    @InjectedParam private val screen: RecipeDetailScreen,
) : StateViewModel<RecipeDetailState, RecipeDetailUiState>() {
    override fun createInitialState() = RecipeDetailState()

    override fun RecipeDetailState.toUiState(): RecipeDetailUiState =
        RecipeDetailUiState(
            content = toContent(),
            onClickBack = ::navigateBack,
        )

    private fun RecipeDetailState.toContent(): RecipeDetailUiState.Content =
        when {
            loading -> RecipeDetailUiState.Content.Loading
            recipe != null -> RecipeDetailUiState.Content.Loaded(recipe)
            else -> RecipeDetailUiState.Content.Error(onClickRetry = ::load)
        }

    init {
        load()
    }

    private fun load() {
        innerState.update { it.copy(loading = true, recipe = null) }
        viewModelScope.launch {
            recipeRepository
                .get(screen.sessionId, screen.recipeId)
                .onOk { recipe ->
                    log.i { "Loaded recipe: ${recipe.id}" }
                    innerState.update { it.copy(loading = false, recipe = recipe) }
                }.onErr { error ->
                    log.e { "Failed to load recipe ${screen.recipeId}: $error" }
                    innerState.update { it.copy(loading = false, recipe = null) }
                }
        }
    }

    private fun navigateBack() {
        navigator.pop()
    }
}

data class RecipeDetailState(
    val loading: Boolean = true,
    val recipe: Recipe? = null,
)

data class RecipeDetailUiState(
    val content: Content,
    val onClickBack: () -> Unit,
) {
    sealed interface Content {
        data object Loading : Content

        data class Error(
            val onClickRetry: () -> Unit,
        ) : Content

        data class Loaded(
            val recipe: Recipe,
        ) : Content
    }
}
