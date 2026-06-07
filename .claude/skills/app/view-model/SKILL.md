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

1. **Constructor arguments** — only declare them as `val` if necessary to store their value. Always private in that case.
2. **Private values derived from constructor arguments** — such as navigation parameters (session ID).
3. **Cancellable job references** — for async jobs that might need restarting or cancelling, such as streams, hold each `Job` directly in the ViewModel in an atomic reference, not in `State`:
    ```kotlin
    private val sessionJob = atomic<Job?>(null)
    ```
4. **`private val innerState = MutableStateFlow(State())`**.
5. **`val uiState: StateFlow<UiState>`** — derived from `innerState` and mapped to a `StateFlow` with a subscription time-limited `SharingStarted`:
    ```kotlin
    val uiState: StateFlow<UiState> =
        innerState
            .map { it.toUiState() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), innerState.value.toUiState())
    ```
6. **`private fun State.toUiState(): UiState`** — pure mapping from state to UI state, wiring callbacks.
7. **Private state to UI state mapping functions** — to avoid making `toUiState` too complex, it can be broken down into smaller functions declared right after it.
8. **`init { ... }`** — Perform init logic such as launching long-running collectors.
9. **Private action functions** — Additional utility functions called as part of `init` or UI state callbacks.

### Top-level after ViewModel class

1. **`private data class State`** — the single source of truth for state in the ViewModel. All properties are immutable (mutation is done via copy functions); every field has a default unless they are set via ViewModel constructor arguments.
2. **`UiState`** — what the UI renders: UI-friendly data + callbacks. See the *State vs UiState* section below.
3. **Public UI models** — additional models that are part of `UiState`; same guidelines apply.

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
If parts of the UI are not always present, use nullable properties in the UI state.

## Callbacks

- Put in the UiState model closest to the UI component that will trigger the callback.
- Name by interaction: `onClick<Thing>`, `on<Field>Changed`, etc.
- Type as `() -> Unit` when possible (or `(T) -> Unit` if the input comes from the UI, such as text changes).
- **Make a callback nullable (`(() -> Unit)?`) to express "disabled"** — return `null` from `toUiState()` (or a `get()`) when the action isn't currently allowed (blank input, not connected, etc.). The UI greys out the control. Don't add separate `enabled` booleans for this.
- When assigning callbacks **into the `UiState` hierarchy**, NEVER use lambdas, as they will generate a new equals value each time, causing unnecessary recompositions. Instead, use references to private functions (`::addItem`).

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
- For a stream that should be cancelled and restarted on demand (e.g. login/logout), store its `Job` directly in the ViewModel in an atomic reference (not in `State`). Replace it with an atomic function (e.g. `getAndSet`) and cancel the previous one in the same step, so concurrent calls can't leak a job:
    ```kotlin
    sessionJob.getAndSet(viewModelScope.launch { /* collect */ })?.cancel()
    ```

## Avoiding unwanted async operations

To avoid unwanted async operations due to accidental double-clicks and similar, introduce a flag in the state to indicate that an operation is in progress, and use that flag to disable the callback.
This flag should be set to `true` before starting the operation and set to `false` in a `finally` block after the operation completes. The try-finally block might need to run in the new coroutine.

## Conventions

- Modern Kotlin, immutable data, functional patterns. No more code than necessary.
- Write unit-tests using the unit-test skill.
- After changes, run the **verify** skill.

## Example

See `app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/screens/ingredients/IngredientsViewModel.kt` for a good example.
