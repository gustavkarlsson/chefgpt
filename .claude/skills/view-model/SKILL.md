---
name: view-model
description: Create or modify a ViewModel in the `app` module. Use this skill whenever adding a new screen ViewModel or changing an existing one.
---

ViewModels live in `app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/screens/<screen>/`
Their names match their accompanying screen. Some examples:
- `UserScreen` -> `UserViewModel`
- `SettingsScreen` -> `SettingsViewModel`
- `LoginScreen` -> `LoginViewModel`

## Structure

All view models should follow the same structure and patterns.

Here are the key concepts and the order in which they should appear in the file:

### Top-level before ViewModel class

1. **Top-level logger**, keyed off the class name:
    ```kotlin
    private val log = Logger.withTag("${MyViewModel::class.simpleName}")
    ```
2. **Private constants** — used by the ViewModel. Not necessarily Kotlin constants, but immutable values used in the file.

### Inside ViewModel class

1. **`private val innerState = MutableStateFlow(State())`**.
2. **`val UiState: StateFlow<UiState>`** — derived from `innerState` via `toUiState()`.
3. **`private fun State.toUiState(): UiState`** — pure mapping from state to UI state, wiring callbacks.
4. **Private state to UI state mapping functions** — to avoid making `toUiState` too complex, it can be broken down into smaller functions declared right after it.
5. **`init { ... }`** — Perform init logic such as launching long-running collectors.
6. **Private action functions** — Additional utility functions called as part of `init` or UI state callbacks.

### Top-level after ViewModel class

1. **`private data class State`** — the single source of truth for state in the ViewModel. All properties are immutable (mutation is done via copy functions); every field has a default unless they are set via ViewModel constructor arguments.
2. **`UiState`** — what the UI renders. UI-friendly data + callbacks. Never exposes domain/api types; map them to view-local types. Use sealed interface hierarchies when multiple different states are needed. The structure should match the intended UI so that parts of the UiState can be passed to composables.
3. **Public UI models** — additional models that are part of `UiState`; same guidelines apply.

## Example

```kotlin
private val log = Logger.withTag("${ExampleViewModel::class.simpleName}")

private const val MAX_INGREDIENTS = 10

class ExampleViewModel(
    private val repository: IngredientsRepository,
    private val navigator: Navigator,
    @InjectedParam private val route: Route.Example,
) : ViewModel() {
    private val innerState = MutableStateFlow(State())

    val uiState: StateFlow<UiState> =
        innerState
            .map { it.toUiState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, innerState.value.toUiState())

    private fun State.toUiState(): UiState =
        if (ingredients == null) {
            UiState.Loading
        } else {
            ingredients.mapBoth(
                success = {
                    UiState.Loaded(
                        ingredients = it.map { it.toItem() },
                        inputText = inputText,
                        onClickSave = ::saveIngredients,
                        onClickBack = { navigator.pop() },
                    )
                },
                failure = { UiState.Error(message = "Failed to load ingredients", onClickRetry = ::loadIngredients) },
            )
        }

    private fun ApiIngredient.toItem(): UiIngredient {
        return UiIngredient(
            name = name,
            onClickRemove = { removeIngredient(id) },
        )
    }

    init {
        loadIngredients()
    }

    private fun loadIngredients() {
        viewModelScope.launch {
            val ingredients = repository.loadIngredients(route.sessionId)
            innerState.update { it.copy(ingredients = ingredients) }
        }
    }

    private fun saveIngredients() {
        val previousState = innerState.getAndUpdate {
            it.copy(savingIngredients = true)
        }
        if (!previousState.savingIngredients && previousState.ingredients?.isOk == true) {
            viewModelScope.launch {
                try {
                    val ingredients = previousState.ingredients.get().orEmpty()
                    repository
                        .save(ingredients)
                        .onOk { log.i { "Saved ${ingredients.size} ingredients" } }
                        .onErr { log.e { "Failed to save ${ingredients.size} ingredients: ${it.errorBody}" } }
                } finally {
                    innerState.update { it.copy(savingIngredients = false) }
                }
            }
        }
    }

    private fun removeIngredient(id: IngredientId) {
        innerState.update { it.copy(inputText = "") }
        viewModelScope.launch {
            repository
                .remove(id)
                .onErr { log.e { "Failed to add '$name': ${it.errorBody}" } }
        }
    }
}

private data class State(
    val ingredients: Result<List<ApiIngredient>, ApiError>? = null,
    val savingIngredients: Boolean = false,
    val inputText: String = "",
)

sealed interface UiState {
    data object Loading : UiState
    data class Loaded(
        val ingredients: List<UiIngredient>,
        val inputText: String,
        val onClickSave: (() -> Unit)?,
        val onClickBack: () -> Unit,
    ) : UiState

    data class Error(
        val message: String,
        val onClickRetry: () -> Unit,
    ) : UiState
}

data class UiIngredient(
    val name: String,
    val onClickRemove: () -> Unit,
)

```

## State vs UiState

### State

State is internal and holds raw unprocessed data such as:
- Backend data
- Text field state
- Async results
- Loading flags

Keep the fields close to the source. For data that needs to load async, declare the field nullable and use `null` to indicate loading.

### UiState

UiState is the ViewModel's contract with the UI. It contains data that's ready to be presented by the UI, such as:

- Complex values formatted as strings
- Nested states, made to represent the UI
- Callbacks for events such as clicks, back presses, or entering text

The data should be prepared in a way that the UI has to perform a minimal amount of logic. Sorting, filtering, and other transformations should be done in the ViewModel, not the UI.
The structure should mimic the UI hierarchy. If a part of the UI can be in different mutually exclusive states, use a `sealed interface` hierarchy for the state. If UI components are nested inside other components, use nested properties in the UI state.
If parts of the UI is not always present, use nullable properties in the UI state.

## Callbacks

- Put in the UiState model closest to the UI component that will trigger the callback.
- Name by interaction: `onClick<Thing>`, `on<Field>Changed`, etc.
- Type as `() -> Unit` when possible (or `(T) -> Unit` if the input comes from the UI, such as text changes).
- **Make a callback nullable (`(() -> Unit)?`) to express "disabled"** — return `null` from `toUiState()` (or a `get()`) when the action isn't currently allowed (blank input, not connected, etc.). The UI greys out the control. Don't add separate `enabled` booleans for this.
- Reference a private function (`::addItem`) for non-trivial actions; inline a lambda for trivial actions such as state updates and navigation.

## Actions

- Each user action is a `private fun`. Side-effecting work runs in `viewModelScope.launch { ... }`.
- Repositories/clients return results (`com.github.michaelbull.result.Result`). Handle both branches with `.onOk { }` / `.onErr { }`; log failures via `log.e`. Never swallow errors silently.
- Update `innerState` with `.update { it.copy(...) }`. Use `.getAndUpdate { }` when you need the pre-update snapshot (e.g. clearing an input while keeping its value to send).

## Dependencies & DI

- Don't construct dependencies inside the ViewModel.
- Inject collaborators (repositories, `ChefGptClient`, `Navigator`, factories) as constructor params, `private val` or without a backing field if possible. Inject interfaces instead of concrete implementation when available.
- Navigation route arguments come in via `@InjectedParam private val route: Route.X`.

## Long-running streams

- Start collectors in `init` on `viewModelScope`.
- For a stream that should be cancelled and restarted on demand (e.g. login/logout), keep a `Job` in the state and replace it.

## Avoiding unwanted async operations

To avoid unwanted async operations due to accidental double-clicks and similar, introduce a flag in the state to indicate that an operation is in progress, and use that flag to disable the callback.
This flag should be set to `true` before starting the operation and set to `false` in a `finally` block after the operation completes.  The try-finally block might need to run in the new coroutine.

## Conventions

- Modern Kotlin, immutable data, functional patterns. No more code than necessary.
- Write unit-tests using the unit-test skill.
- After changes, run the **verify** skill.
