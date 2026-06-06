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
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.IngredientId
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
        val backendIngredients: List<ApiIngredient> = emptyList(),
        // Optimistic inventory overrides by name, dropped once the backend agrees.
        val overrides: Map<String, Boolean> = emptyMap(),
        val inputText: String = "",
    )

    data class Ingredient(
        val id: IngredientId,
        val name: String,
        val inInventory: Boolean,
    )

    inner class ViewState(
        // Ingredients currently in store, most recently modified last.
        val inStore: List<Ingredient>,
        // Ingredients that have previously been in store, most recently modified last.
        val previouslyInStore: List<Ingredient>,
        val inputText: String,
        val onClickIngredient: (Ingredient) -> Unit,
        val onDestroyIngredient: (Ingredient) -> Unit,
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
                        innerState.update { state ->
                            state.copy(
                                backendIngredients = ingredients,
                                // Drop overrides the backend has caught up with.
                                overrides =
                                    state.overrides.filterNot { (name, inInventory) ->
                                        ingredients.firstOrNull { it.name == name }?.inInventory == inInventory
                                    },
                            )
                        }
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

    private fun State.toViewState(): ViewState {
        val sorted =
            backendIngredients
                .sortedBy { it.lastModified }
                .map { Ingredient(id = it.id, name = it.name, inInventory = overrides[it.name] ?: it.inInventory) }
        return ViewState(
            inStore = sorted.filter { it.inInventory },
            previouslyInStore = sorted.filterNot { it.inInventory },
            inputText = inputText,
            onClickIngredient = ::onClickIngredient,
            onDestroyIngredient = ::destroyIngredient,
            onClickBack = { navigator.pop() },
        )
    }

    private fun onClickIngredient(ingredient: Ingredient) {
        val target = !ingredient.inInventory
        // Creation is by name; removal is by id.
        innerState.update { it.copy(overrides = it.overrides + (ingredient.name to target)) }
        viewModelScope.launch {
            val result =
                if (target) {
                    client.addIngredient(route.sessionId, ingredient.name)
                } else {
                    client.removeIngredient(route.sessionId, ingredient.id)
                }
            result.onErr {
                val verb = if (target) "add" else "remove"
                log.e { "Failed to $verb ingredient '${ingredient.name}': ${it.errorBody}" }
            }
        }
    }

    private fun destroyIngredient(ingredient: Ingredient) {
        // Optimistically drop it entirely; the backend stream will confirm.
        innerState.update {
            it.copy(
                backendIngredients = it.backendIngredients.filterNot { stored -> stored.id == ingredient.id },
                overrides = it.overrides - ingredient.name,
            )
        }
        viewModelScope.launch {
            client
                .removeIngredient(route.sessionId, ingredient.id, destroy = true)
                .onErr { log.e { "Failed to destroy ingredient '${ingredient.name}': ${it.errorBody}" } }
        }
    }

    private fun addIngredient(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        innerState.update { it.copy(inputText = "", overrides = it.overrides + (trimmed to true)) }
        viewModelScope.launch {
            client
                .addIngredient(route.sessionId, trimmed)
                .onErr { log.e { "Failed to add ingredient '$trimmed': ${it.errorBody}" } }
        }
    }
}
