package se.gustavkarlsson.chefgpt.screens.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import org.kodein.emoji.Emoji
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.ingredients.IngredientEmojiResolver
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.Route
import kotlin.time.Duration.Companion.seconds

private val log = Logger.withTag("${IngredientsViewModel::class.simpleName}")

class IngredientsViewModel(
    private val client: ChefGptClient,
    private val navigator: Navigator,
    private val emojiResolverFactory: IngredientEmojiResolver.Factory,
    @InjectedParam private val route: Route.Ingredients,
) : ViewModel() {
    private data class State(
        // Latest ingredients reported by the backend stream.
        val backendIngredients: List<ApiIngredient> = emptyList(),
        val inputText: String = "",
        // True while an image is being scanned for ingredients by the backend.
        val scanningImage: Boolean = false,
        // Resolves ingredient emoji; null until the emoji catalog has loaded.
        val emojiResolver: IngredientEmojiResolver? = null,
    )

    data class Ingredient(
        val id: IngredientId,
        val name: String,
        val inInventory: Boolean,
        val emoji: Emoji?,
    )

    inner class ViewState(
        // Ingredients currently in store, most recently modified last.
        val inStore: List<Ingredient>,
        // Ingredients that have previously been in store, most recently modified last.
        val previouslyInStore: List<Ingredient>,
        val inputText: String,
        // True while an image is being scanned for ingredients.
        val scanningImage: Boolean,
        val onClickIngredient: (Ingredient) -> Unit,
        val onDestroyIngredient: (Ingredient) -> Unit,
        val onClickBack: () -> Unit,
        val onScanImageSelected: (Path) -> Unit,
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
            val resolver = emojiResolverFactory.create()
            innerState.update { it.copy(emojiResolver = resolver) }
        }
        viewModelScope.launch {
            while (true) {
                try {
                    client.listenToIngredients(route.sessionId).collect { ingredients ->
                        innerState.update { it.copy(backendIngredients = ingredients) }
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
        // Hold off on showing ingredients until the emoji catalog is ready, so each renders with its emoji.
        val sorted =
            if (emojiResolver == null) {
                emptyList()
            } else {
                backendIngredients
                    .sortedBy { it.lastModified }
                    .map {
                        Ingredient(
                            id = it.id,
                            name = it.name,
                            inInventory = it.inInventory,
                            emoji = emojiResolver.resolve(it.name),
                        )
                    }
            }
        return ViewState(
            inStore = sorted.filter { it.inInventory },
            previouslyInStore = sorted.filterNot { it.inInventory },
            inputText = inputText,
            scanningImage = scanningImage,
            onClickIngredient = ::onClickIngredient,
            onDestroyIngredient = ::destroyIngredient,
            onClickBack = { navigator.pop() },
            onScanImageSelected = ::scanImage,
        )
    }

    private fun onClickIngredient(ingredient: Ingredient) {
        val target = !ingredient.inInventory
        // Creation is by name; removal is by id.
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
        viewModelScope.launch {
            client
                .removeIngredient(route.sessionId, ingredient.id, destroy = true)
                .onErr { log.e { "Failed to destroy ingredient '${ingredient.name}': ${it.errorBody}" } }
        }
    }

    private fun scanImage(image: Path) {
        if (innerState.value.scanningImage) return
        innerState.update { it.copy(scanningImage = true) }
        viewModelScope.launch {
            try {
                val extension = image.toString().substringAfterLast('.')
                client
                    .scanIngredients(route.sessionId, image, ContentType("image", extension))
                    .onOk { count -> log.i { "Scan found $count ingredient(s)" } }
                    .onErr { log.e { "Failed to scan ingredients: ${it.errorBody}" } }
            } finally {
                innerState.update { it.copy(scanningImage = false) }
            }
        }
    }

    private fun addIngredient(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        innerState.update { it.copy(inputText = "") }
        viewModelScope.launch {
            // Turn a pasted emoji glyph into its alias (e.g. "🍌" -> "banana") so the backend stores a name.
            val resolved = emojiResolverFactory.create().resolveAlias(trimmed) ?: trimmed
            client
                .addIngredient(route.sessionId, resolved)
                .onErr { log.e { "Failed to add ingredient '$resolved': ${it.errorBody}" } }
        }
    }
}
