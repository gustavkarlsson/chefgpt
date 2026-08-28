package se.gustavkarlsson.chefgpt.screens.recipe

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.RecipeId
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
            loading -> {
                RecipeDetailUiState.Content.Loading
            }

            recipe != null -> {
                RecipeDetailUiState.Content.Loaded(
                    recipe = recipe,
                    onClickToggleFavorite = ::toggleFavorite,
                    modificationActions = toModificationActions(),
                )
            }

            else -> {
                RecipeDetailUiState.Content.Error(onClickRetry = ::load)
            }
        }

    private fun RecipeDetailState.toModificationActions(): RecipeDetailUiState.ModificationActions? {
        if (recipe?.modifiedFrom == null) return null
        return RecipeDetailUiState.ModificationActions(
            onClickOverwriteOriginal = if (resolving) null else ::overwriteOriginal,
            onClickSaveAsCopy = if (resolving) null else ::saveAsCopy,
            onClickDiscard = if (resolving) null else ::discard,
        )
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

    private fun toggleFavorite() {
        val recipe = innerState.value.recipe ?: return
        val favorite = !recipe.favorite
        setFavorite(recipe, favorite)
        viewModelScope.launch {
            recipeRepository
                .setFavorite(screen.sessionId, recipe.id, favorite)
                .onErr { error ->
                    log.e { "Failed to set favorite=$favorite on recipe ${recipe.id}: $error" }
                    setFavorite(recipe, !favorite)
                    val message = if (favorite) "Couldn't favorite recipe" else "Couldn't unfavorite recipe"
                    showSnackbar(message, isError = true)
                }
        }
    }

    // Only updates the recipe if it is still the one shown, to avoid clobbering a reload.
    private fun setFavorite(
        recipe: Recipe,
        favorite: Boolean,
    ) {
        innerState.update { state ->
            if (state.recipe?.id == recipe.id) state.copy(recipe = state.recipe.copy(favorite = favorite)) else state
        }
    }

    private fun overwriteOriginal() {
        resolveModification("Couldn't overwrite the original recipe") { recipeId ->
            recipeRepository.overwriteOriginal(screen.sessionId, recipeId)
        }
    }

    private fun saveAsCopy() {
        resolveModification("Couldn't save the recipe as a copy") { recipeId ->
            recipeRepository.saveAsCopy(screen.sessionId, recipeId)
        }
    }

    private fun resolveModification(
        errorMessage: String,
        resolve: suspend (RecipeId) -> Result<Recipe, ClientError>,
    ) {
        val recipeId = innerState.value.recipe?.id ?: return
        innerState.update { it.copy(resolving = true) }
        viewModelScope.launch {
            resolve(recipeId)
                .onOk { resolved ->
                    log.i { "Resolved modified recipe: $recipeId" }
                    innerState.update { it.copy(resolving = false, recipe = resolved) }
                }.onErr { error ->
                    log.e { "Failed to resolve modified recipe $recipeId: $error" }
                    innerState.update { it.copy(resolving = false) }
                    showSnackbar(errorMessage, isError = true)
                }
        }
    }

    private fun discard() {
        val recipeId = innerState.value.recipe?.id ?: return
        innerState.update { it.copy(resolving = true) }
        viewModelScope.launch {
            recipeRepository
                .delete(screen.sessionId, recipeId)
                .onOk {
                    log.i { "Discarded modified recipe: $recipeId" }
                    navigateBack()
                }.onErr { error ->
                    log.e { "Failed to discard modified recipe $recipeId: $error" }
                    innerState.update { it.copy(resolving = false) }
                    showSnackbar("Couldn't discard the changes", isError = true)
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
    val resolving: Boolean = false,
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
            val onClickToggleFavorite: () -> Unit,
            // Set only for a recipe that is a modified version of another one.
            val modificationActions: ModificationActions? = null,
        ) : Content
    }

    // Null callbacks while a resolution is in flight.
    data class ModificationActions(
        val onClickOverwriteOriginal: (() -> Unit)?,
        val onClickSaveAsCopy: (() -> Unit)?,
        val onClickDiscard: (() -> Unit)?,
    )
}
