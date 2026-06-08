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
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.ingredients.EmojiAvatarModel
import se.gustavkarlsson.chefgpt.ingredients.IngredientEmojiResolver
import se.gustavkarlsson.chefgpt.ingredients.IngredientWords
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.Route
import se.gustavkarlsson.chefgpt.sessions.SessionId
import kotlin.time.Duration.Companion.seconds

private val log = Logger.withTag("${IngredientsViewModel::class.simpleName}")

class IngredientsViewModel(
    private val client: ChefGptClient,
    private val navigator: Navigator,
    emojiResolverFactory: IngredientEmojiResolver.Factory,
    @InjectedParam route: Route.Ingredients,
) : ViewModel() {
    private val sessionId: SessionId = route.sessionId

    private val innerState = MutableStateFlow(State())

    val uiState: StateFlow<UiState> =
        innerState
            .map { it.toUiState() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), innerState.value.toUiState())

    private fun State.toUiState(): UiState =
        UiState(
            inInventory = ingredients.toUiIngredients(emojiResolver, inInventory = true),
            notInInventory = ingredients.toUiIngredients(emojiResolver, inInventory = false),
            input =
                UiInput(
                    suggestions = toSuggestions(),
                    text = inputText,
                    onTextChange = ::updateInputText,
                    onScanImageSelected = ::scanImage,
                    onClickAdd = if (inputText.isNotBlank() && emojiResolver != null) ::createIngredient else null,
                ),
            onClickBack = navigator::pop,
        )

    private fun State.toSuggestions(): List<UiIngredient> {
        if (inputText.isBlank() || emojiResolver == null) return emptyList()
        val existing = ingredients.mapTo(mutableSetOf()) { it.name.lowercase() }
        return IngredientWords
            .match(inputText)
            .filterNot { it in existing }
            .map { name ->
                UiIngredient(
                    key = name,
                    name = name,
                    icon = EmojiAvatarModel.of(emojiResolver.resolve(name), name),
                    dimmed = false,
                    onClick = ::addSuggestion,
                    onClickDestroy = null,
                )
            }
    }

    private fun List<ApiIngredient>.toUiIngredients(
        emojiResolver: IngredientEmojiResolver?,
        inInventory: Boolean,
    ): List<UiIngredient> {
        if (emojiResolver == null) return emptyList()
        return this
            .filter { it.inInventory == inInventory }
            .sortedBy { it.lastModified }
            .map { ingredient ->
                UiIngredient(
                    key = ingredient.id.toString(),
                    name = ingredient.name,
                    icon = EmojiAvatarModel.of(emojiResolver.resolve(ingredient.name), ingredient.name),
                    dimmed = !inInventory,
                    onClick = if (inInventory) ::removeIngredient else ::addIngredient,
                    onClickDestroy = if (inInventory) null else ::destroyIngredient,
                )
            }
    }

    init {
        viewModelScope.launch {
            val resolver = emojiResolverFactory.create()
            innerState.update { it.copy(emojiResolver = resolver) }
        }
        viewModelScope.launch {
            while (true) {
                try {
                    client.listenToIngredients(sessionId).collect { ingredients ->
                        innerState.update { it.copy(ingredients = ingredients) }
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

    private fun createIngredient() {
        val previousState =
            innerState.getAndUpdate { state ->
                if (state.inputText.isBlank() || state.emojiResolver == null) {
                    // Abort if input is blank or emoji resolver is not ready
                    return
                } else {
                    state.copy(inputText = "")
                }
            }
        val trimmed = previousState.inputText.trim()
        val emojiResolver = checkNotNull(previousState.emojiResolver)
        // Turn a pasted emoji glyph into its alias (e.g. "🍌" -> "banana") so the backend stores a name.
        create(emojiResolver.resolveAlias(trimmed) ?: trimmed)
    }

    private fun addSuggestion(name: String) {
        innerState.update { it.copy(inputText = "") }
        create(name)
    }

    private fun create(name: String) {
        viewModelScope.launch {
            val result = client.createIngredient(sessionId, name)
            result.onErr { log.e { "Failed to create ingredient '$name': $it" } }
        }
    }

    private fun destroyIngredient(key: String) {
        val id = IngredientId.parse(key)
        viewModelScope.launch {
            val result = client.destroyIngredient(sessionId, id)
            result.onErr {
                log.e { "Failed to destroy ingredient $id: $it" }
            }
        }
    }

    private fun addIngredient(key: String) {
        val id = IngredientId.parse(key)
        viewModelScope.launch {
            val result = client.setIngredientInventory(sessionId, id, inInventory = true)
            result.onErr {
                log.e { "Failed to add ingredient $id: $it" }
            }
        }
    }

    private fun removeIngredient(key: String) {
        val id = IngredientId.parse(key)
        viewModelScope.launch {
            val result = client.setIngredientInventory(sessionId, id, inInventory = false)
            result.onErr {
                log.e { "Failed to remove ingredient $id: $it" }
            }
        }
    }

    private fun updateInputText(text: String) {
        innerState.update { it.copy(inputText = text) }
    }

    private fun scanImage(image: Path) {
        if (innerState.value.scanningImage) return // Already scanning
        innerState.update { it.copy(scanningImage = true) }
        viewModelScope.launch {
            try {
                val extension = image.toString().substringAfterLast('.')
                client
                    .scanIngredients(sessionId, image, ContentType("image", extension))
                    .onOk { count -> log.i { "Scan found $count ingredient(s)" } }
                    .onErr { log.e { "Failed to scan ingredients: $it" } }
            } finally {
                innerState.update { it.copy(scanningImage = false) }
            }
        }
    }
}

// TODO Mark updates as "new"
private data class State(
    val ingredients: List<ApiIngredient> = emptyList(),
    val inputText: String = "",
    val scanningImage: Boolean = false,
    val emojiResolver: IngredientEmojiResolver? = null, // null until the emoji catalog has loaded.
)

data class UiState(
    val inInventory: List<UiIngredient>,
    val notInInventory: List<UiIngredient>,
    val input: UiInput,
    val onClickBack: () -> Unit,
)

data class UiInput(
    val suggestions: List<UiIngredient>,
    val text: String,
    val onTextChange: (String) -> Unit,
    val onScanImageSelected: ((Path) -> Unit)?,
    val onClickAdd: (() -> Unit)?,
)

data class UiIngredient(
    // Stable identity and click argument: the ingredient id for stored items, the word itself for suggestions.
    val key: String,
    val name: String,
    val icon: EmojiAvatarModel,
    val dimmed: Boolean,
    val onClick: (String) -> Unit,
    val onClickDestroy: ((String) -> Unit)?,
)
