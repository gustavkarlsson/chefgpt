package se.gustavkarlsson.chefgpt.screens.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.Route
import kotlin.time.Duration.Companion.seconds

private val log = Logger.withTag("${IngredientsViewModel::class.simpleName}")

class IngredientsViewModel(
    private val client: ChefGptClient,
    private val navigator: Navigator,
    @InjectedParam private val route: Route.Ingredients,
) : ViewModel() {
    private data class State(
        // Latest ingredients reported by the backend stream.
        val backendIngredients: Set<String> = emptySet(),
        // Locally removed ingredients, kept visible in a "removed" state.
        val removed: Set<String> = emptySet(),
        val inputText: String = "",
    )

    data class Ingredient(
        val name: String,
        val removed: Boolean,
    )

    inner class ViewState(
        val ingredients: List<Ingredient>,
        val inputText: String,
        val onClickIngredient: (String) -> Unit,
        val onClickBack: () -> Unit,
    ) {
        val onInputChange: (String) -> Unit
            get() = { text -> innerState.update { it.copy(inputText = text) } }

        val onClickAdd: (() -> Unit)?
            get() = if (inputText.isNotBlank()) ({ addIngredient(inputText) }) else null
    }

    private val innerState = MutableStateFlow(State())

    val viewState: StateFlow<ViewState> =
        innerState
            .map { it.toViewState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, innerState.value.toViewState())

    init {
        viewModelScope.launch {
            while (true) {
                try {
                    client.listenToIngredients(route.sessionId).collect { ingredients ->
                        innerState.update { it.copy(backendIngredients = ingredients.toSet()) }
                    }
                    log.e { "Ingredient stream ended" }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(e) { "Ingredient stream failed" }
                } finally {
                    delay(1.seconds)
                }
            }
        }
    }

    private fun State.toViewState(): ViewState =
        ViewState(
            ingredients =
                (backendIngredients + removed)
                    .sortedBy { it.lowercase() }
                    .map { Ingredient(name = it, removed = it in removed) },
            inputText = inputText,
            onClickIngredient = ::onClickIngredient,
            onClickBack = { navigator.pop() },
        )

    private fun onClickIngredient(name: String) {
        val wasRemoved = name in innerState.value.removed
        if (wasRemoved) {
            // Optimistically restore so the card doesn't flicker before the stream catches up.
            innerState.update {
                it.copy(removed = it.removed - name, backendIngredients = it.backendIngredients + name)
            }
            addIngredient(name)
        } else {
            innerState.update { it.copy(removed = it.removed + name) }
            viewModelScope.launch {
                client
                    .removeIngredient(route.sessionId, name)
                    .onErr { log.e { "Failed to remove ingredient '$name': ${it.errorBody}" } }
            }
        }
    }

    private fun addIngredient(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        innerState.update { it.copy(removed = it.removed - trimmed, inputText = "") }
        viewModelScope.launch {
            client
                .addIngredient(route.sessionId, trimmed)
                .onErr { log.e { "Failed to add ingredient '$trimmed': ${it.errorBody}" } }
        }
    }
}
