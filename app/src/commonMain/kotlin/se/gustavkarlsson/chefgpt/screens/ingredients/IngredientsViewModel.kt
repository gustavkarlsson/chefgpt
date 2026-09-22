package se.gustavkarlsson.chefgpt.screens.ingredients

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.http.ContentType
import io.ktor.http.defaultForFilePath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.kodein.emoji.Emoji
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.DeviceConfig
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.ingredients.EmojiAvatarModel
import se.gustavkarlsson.chefgpt.ingredients.IngredientWords
import se.gustavkarlsson.chefgpt.ingredients.usecases.CreateIngredient
import se.gustavkarlsson.chefgpt.ingredients.usecases.DestroyIngredient
import se.gustavkarlsson.chefgpt.ingredients.usecases.ResolveEmoji
import se.gustavkarlsson.chefgpt.ingredients.usecases.ResolveEmojiAlias
import se.gustavkarlsson.chefgpt.ingredients.usecases.ScanIngredients
import se.gustavkarlsson.chefgpt.ingredients.usecases.SetIngredientInventory
import se.gustavkarlsson.chefgpt.ingredients.usecases.StreamIngredients
import se.gustavkarlsson.chefgpt.jobs.usecases.AwaitJob
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.StateViewModel
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar
import kotlin.time.Duration.Companion.seconds

private val log = Logger.withTag("${IngredientsViewModel::class.simpleName}")

private const val EMPTY_HEADLINE = "Your kitchen is empty"
private const val EMPTY_DESCRIPTION =
    "Type the ingredients you have in the field below and I'll help you cook something up."

class IngredientsViewModel(
    private val streamIngredients: StreamIngredients,
    private val createIngredient: CreateIngredient,
    private val destroyIngredient: DestroyIngredient,
    private val setIngredientInventory: SetIngredientInventory,
    private val scanIngredients: ScanIngredients,
    private val awaitJob: AwaitJob,
    private val resolveEmoji: ResolveEmoji,
    private val resolveEmojiAlias: ResolveEmojiAlias,
    private val showSnackbar: ShowSnackbar,
    private val navigator: Navigator,
    private val deviceConfig: DeviceConfig,
    @InjectedParam screen: IngredientsScreen,
) : StateViewModel<State, UiState>() {
    private val sessionId: SessionId = screen.sessionId

    // One-shot events telling the UI to focus the input, fired whenever the ingredient
    // list goes empty (including the first load arriving empty).
    private val focusInputChannel = Channel<Unit>(Channel.UNLIMITED)
    val focusInputEvents: Flow<Unit> = focusInputChannel.receiveAsFlow()

    override fun createInitialState() =
        State(
            supportsCamera = deviceConfig.supportsCamera,
            ingredients = null,
            inputText = "",
            scanningImage = false,
            emojiByIngredient = emptyMap(),
            baselineInInventory = null,
        )

    override fun State.toUiState(): UiState =
        UiState(
            content = toUiContent(),
            input =
                UiInput(
                    text = inputText,
                    onTextChange = ::updateInputText,
                    cameraButton =
                        if (supportsCamera) {
                            UiCameraButton(
                                scanningImage = scanningImage,
                                onPhotoTaken = ::scanImage,
                                onError = ::showPhotoError,
                            )
                        } else {
                            null
                        },
                    onClickAdd = if (inputText.isNotBlank()) ::addIngredientFromInput else null,
                ),
            onClickBack = navigator::pop,
        )

    // With nothing in stock or suggested, invite the user to type their ingredients into the input.
    private fun State.toUiContent(): UiContent {
        // Hold off on the empty state until the first ingredient emission has arrived.
        if (ingredients == null) return UiContent.Loading
        val inInventory =
            ingredients.toUiIngredients(
                emojiByIngredient,
                inInventory = true,
                baseline = baselineInInventory,
            )
        // While the user is typing, the second section shows matching suggestions; otherwise it
        // falls back to the ingredients that were previously in store.
        val secondSection =
            if (inputText.isNotBlank()) {
                IngredientSection(title = "Suggestions", ingredients = toSuggestions())
            } else {
                IngredientSection(
                    title = "Previously in store",
                    ingredients =
                        ingredients.toUiIngredients(
                            emojiByIngredient,
                            inInventory = false,
                            baseline = baselineInInventory,
                        ),
                )
            }
        return if (inInventory.isEmpty() && secondSection.ingredients.isEmpty()) {
            UiContent.Empty(headline = EMPTY_HEADLINE, description = EMPTY_DESCRIPTION)
        } else {
            UiContent.Ingredients(inInventory = inInventory, secondSection = secondSection)
        }
    }

    private fun State.toSuggestions(): List<UiIngredient> {
        if (inputText.isBlank()) return emptyList()
        val needle = inputText.trim().lowercase()

        // Previously in-store ingredients that match can be moved straight back into the inventory.
        val previouslyInStore =
            ingredients
                .orEmpty()
                .filterNot { it.inInventory }
                .filter { it.name.lowercase().contains(needle) }
                .sortedBy { it.lastModified }
                .map { ingredient ->
                    UiIngredient(
                        key = ingredient.id.toString(),
                        name = ingredient.name,
                        icon = EmojiAvatarModel.of(emojiByIngredient[ingredient.name], ingredient.name),
                        dimmed = false,
                        isNew = false,
                        onClick = ::addIngredient,
                        onClickDestroy = null,
                    )
                }

        // Catalog words we don't know about yet become brand new ingredients when tapped.
        val existing = ingredients.orEmpty().mapTo(mutableSetOf()) { it.name.lowercase() }
        val newWords =
            IngredientWords.match(inputText).filterNot { it in existing }.map { name ->
                UiIngredient(
                    key = name,
                    name = name,
                    icon = EmojiAvatarModel.of(emojiByIngredient[name], name),
                    dimmed = false,
                    isNew = false,
                    onClick = ::addSuggestion,
                    onClickDestroy = null,
                )
            }

        return previouslyInStore + newWords
    }

    private fun List<ApiIngredient>.toUiIngredients(
        emojiByIngredient: Map<String, Emoji?>,
        inInventory: Boolean,
        baseline: Set<IngredientId>?,
    ): List<UiIngredient> =
        filter { it.inInventory == inInventory }.sortedBy { it.lastModified }.map { ingredient ->
            UiIngredient(
                key = ingredient.id.toString(),
                name = ingredient.name,
                icon = EmojiAvatarModel.of(emojiByIngredient[ingredient.name], ingredient.name),
                dimmed = !inInventory,
                isNew = inInventory && baseline != null && ingredient.id !in baseline,
                onClick = if (inInventory) ::removeIngredient else ::addIngredient,
                onClickDestroy = if (inInventory) null else ::destroyIngredientById,
            )
        }

    init {
        viewModelScope.launch {
            while (true) {
                try {
                    streamIngredients(sessionId).collect { ingredients ->
                        val previous =
                            innerState.getAndUpdate { state ->
                                // The first emission establishes the baseline of what was already in stock.
                                val baseline =
                                    state.baselineInInventory ?: ingredients
                                        .filter { it.inInventory }
                                        .mapTo(mutableSetOf()) { it.id }
                                state.copy(ingredients = ingredients, baselineInInventory = baseline)
                            }
                        resolveEmojisFor(ingredients.map { it.name })
                        // Fire whenever the list transitions to empty; previous is null until the first load.
                        if (ingredients.isEmpty() && previous.ingredients?.isEmpty() != true) {
                            focusInputChannel.send(Unit)
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

    private fun addIngredientFromInput() {
        val previousState =
            innerState.getAndUpdate { state ->
                if (state.inputText.isBlank()) return
                state.copy(inputText = "")
            }
        val trimmed = previousState.inputText.trim()
        viewModelScope.launch {
            // Turn a pasted emoji glyph into its alias (e.g. "🍌" -> "banana") so the backend stores a name.
            val alias = resolveEmojiAlias(trimmed)
            create(alias ?: trimmed)
        }
    }

    private fun addSuggestion(name: String) {
        innerState.update { it.copy(inputText = "") }
        create(name)
    }

    private fun create(name: String) {
        viewModelScope.launch {
            createIngredient(sessionId, name)
                .onErr {
                    log.e { "Failed to create ingredient '$name': $it" }
                    showSnackbar("Couldn't add $name", isError = true)
                }
        }
    }

    private fun destroyIngredientById(key: String) {
        val id = IngredientId.parse(key)
        viewModelScope.launch {
            destroyIngredient(sessionId, id)
                .onErr {
                    log.e { "Failed to destroy ingredient $id: $it" }
                    showSnackbar("Couldn't delete ingredient", isError = true)
                }
        }
    }

    private fun addIngredient(key: String) {
        val id = IngredientId.parse(key)
        viewModelScope.launch {
            setIngredientInventory(sessionId, id, inInventory = true)
                .onErr {
                    log.e { "Failed to add ingredient $id: $it" }
                    showSnackbar("Couldn't move ingredient to your inventory", isError = true)
                }
        }
    }

    private fun removeIngredient(key: String) {
        val id = IngredientId.parse(key)
        viewModelScope.launch {
            setIngredientInventory(sessionId, id, inInventory = false)
                .onErr {
                    log.e { "Failed to remove ingredient $id: $it" }
                    showSnackbar("Couldn't remove ingredient from your inventory", isError = true)
                }
        }
    }

    private fun updateInputText(text: String) {
        innerState.update { it.copy(inputText = text) }
        resolveEmojisFor(IngredientWords.match(text))
    }

    private fun scanImage(image: Path) {
        innerState.update {
            if (it.scanningImage) return // Already scanning
            it.copy(scanningImage = true)
        }
        viewModelScope.launch {
            try {
                awaitJob(
                    sessionId,
                    ListSerializer(String.serializer()),
                ) { scanIngredients(sessionId, image, ContentType.defaultForFilePath(image.name)) }
                    .onOk { job -> log.i { "Scan found ${job.result.orEmpty().size} ingredient(s)" } }
                    .onErr {
                        log.e { "Failed to scan ingredients: $it" }
                        showSnackbar("Couldn't scan ingredients from the image", isError = true)
                    }
            } finally {
                innerState.update { it.copy(scanningImage = false) }
            }
        }
    }

    private fun showPhotoError() {
        showSnackbar("Could not take a photo", isError = true)
    }

    private fun resolveEmojisFor(names: List<String>) {
        val missing = names.filter { it !in innerState.value.emojiByIngredient }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val resolved = mutableMapOf<String, Emoji?>()
            for (name in missing) {
                resolved[name] = resolveEmoji(name)
            }
            innerState.update { state -> state.copy(emojiByIngredient = state.emojiByIngredient + resolved) }
        }
    }
}

data class State(
    val supportsCamera: Boolean,
    val ingredients: List<ApiIngredient>?, // null until the first ingredient emission arrives.
    val inputText: String,
    val scanningImage: Boolean,
    // Ingredient name to resolved emoji; filled lazily as names first appear.
    val emojiByIngredient: Map<String, Emoji?>,
    // Ids in the inventory when the screen opened (first stream emission). Anything in stock now but
    // absent here is "new"; an ingredient removed and re-added returns to the baseline, so it isn't.
    val baselineInInventory: Set<IngredientId>?,
)

data class UiState(
    val content: UiContent,
    val input: UiInput,
    val onClickBack: () -> Unit,
)

sealed interface UiContent {
    data object Loading : UiContent

    data class Empty(
        val headline: String,
        val description: String,
    ) : UiContent

    data class Ingredients(
        val inInventory: List<UiIngredient>,
        val secondSection: IngredientSection,
    ) : UiContent
}

data class IngredientSection(
    val title: String,
    val ingredients: List<UiIngredient>,
)

data class UiInput(
    val text: String,
    val onTextChange: (String) -> Unit,
    val cameraButton: UiCameraButton?,
    val onClickAdd: (() -> Unit)?,
)

data class UiCameraButton(
    val scanningImage: Boolean,
    val onPhotoTaken: (photo: Path) -> Unit,
    val onError: () -> Unit,
)

data class UiIngredient(
    // Stable identity and click argument: the ingredient id for stored items, the word itself for suggestions.
    val key: String,
    val name: String,
    val icon: EmojiAvatarModel,
    val dimmed: Boolean,
    val isNew: Boolean,
    val onClick: (String) -> Unit,
    val onClickDestroy: ((String) -> Unit)?,
)
