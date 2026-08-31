---
name: view-model
description: Create or modify a ViewModel in the `app` module. Use this skill whenever adding a new screen ViewModel or changing an existing one.
---

ViewModels live in `app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/screens/<screen>/`
Their names match their accompanying screen. Some examples:
- `UserScreen` -> `UserViewModel`
- `SettingsScreen` -> `SettingsViewModel`
- `LoginScreen` -> `LoginViewModel`

## Base class

ViewModels extend the abstract `StateViewModel<State, UiState>` in
`app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/screens/StateViewModel.kt`,
which owns the shared boilerplate: the `innerState` holder, the
`innerState → uiState` mapping pipeline, and the `StateFlow` exposure.

The two type parameters are the ViewModel's `State` and `UiState` types (both
must be non-null). Subclasses provide two functions:

- `protected abstract fun createInitialState(): State` — builds the initial state.
- `protected abstract fun State.toUiState(): UiState` — the pure state-to-UI mapping.

`innerState` is inherited (`protected`); mutate it with `innerState.update { }`
as usual. `uiState` is inherited and public. The base also provides
`snackbarMessages` and `showSnackbar(...)` (see *Snackbars* below).

**Init-order rule:** the base creates `innerState` and the `uiState` seed
lazily, so `createInitialState()`/`toUiState()` may reference constructor-injected
dependencies. Do not, however, make `createInitialState()` depend on subclass
properties that are assigned in `init`/initializers.

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

The class extends `StateViewModel<State, UiState>()`.

1. **Constructor arguments** — only declare them as `val` if necessary to store their value. Always private in that case.
2. **Private values derived from constructor arguments** — such as navigation parameters (session ID).
3. **Cancellable job references** — for async jobs that might need restarting or cancelling, such as streams, hold each `Job` directly in the ViewModel in an atomic reference, not in `State`:
    ```kotlin
    private val sessionJob = atomic<Job?>(null)
    ```
4. **`override fun createInitialState(): State`** — names every property of `State`.
5. **`override fun State.toUiState(): UiState`** — pure mapping from state to UI state, wiring callbacks. (`innerState` and `uiState` are inherited from the base.)
6. **Private state to UI state mapping functions** — to avoid making `toUiState` too complex, it can be broken down into smaller functions declared right after it.
7. **`init { ... }`** — Perform init logic such as launching long-running collectors.
8. **Private action functions** — Additional utility functions called as part of `init` or UI state callbacks.

### Top-level after ViewModel class

1. **`data class State`** — a public top-level class (it is the base class's `S` generic argument, so it cannot be `private`). The single source of truth for state in the ViewModel. All properties are immutable (mutation is done via copy functions) and required, so `createInitialState()` spells out every starting value in one place.
2. **`UiState`** — what the UI renders: UI-friendly data + callbacks. See the *State vs UiState* section below. Properties are required here too; `toUiState()` rebuilds the whole thing on every emission.
3. **Public UI models** — additional models that are part of `UiState`; same guidelines apply.

## State vs UiState

### State

State is internal to the ViewModel (conceptually, even though it is a public type) and holds raw unprocessed data such as:
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

## One-shot events

`UiState` describes *what the UI looks like right now*. Some things aren't state — they're discrete, transient occurrences the UI should react to *once*: a value to animate in, a snackbar to show, a navigation triggered by a stream. These don't belong in `UiState` (there's no "current value" to render, and re-emitting the same state shouldn't replay them).

Expose such events as a separate **`Flow`** alongside `uiState`, backed by a `Channel`:

```kotlin
private val ingredientChangeChannel = Channel<IngredientChange>(Channel.UNLIMITED)
val ingredientChanges: Flow<IngredientChange> = ingredientChangeChannel.receiveAsFlow()
```

- Place the `Channel`/`Flow` pair right after `uiState`.
- Push to it from collectors or action functions with `channel.send(...)` (or `trySend`).
- The event type is a public model declared after the ViewModel class, after the UI models.
- The UI collects this `Flow` separately from `uiState` (see the **screen-ui** skill).

`ChatViewModel` (in `screens/chat/`) is a good example: it streams `ingredientChanges` for an animation while everything renderable stays in `uiState`.

### Snackbars

Snackbars (e.g. for surfacing backend errors) are one-shot events, but don't hand-roll a `Channel` for them — the base `StateViewModel` already provides this. It exposes a public `snackbarMessages: Flow<SnackbarMessage>` and a `protected fun showSnackbar(...)`. Just call `showSnackbar(...)` from action functions or collectors; no field to declare:

```kotlin
// ...in an error branch:
result.onErr {
    log.e { "Failed to add $name: $it" }
    showSnackbar("Couldn't add $name", isError = true)
}
```

`showSnackbar(text, isError)` covers the common case (`isError = true` is error-styled and stays until dismissed). For control over `dismissText` or `duration`, use `SnackbarMessages.show(...)`, whose parameters default the same way. The UI renders it via `rememberSnackbarHostState` + `SnackbarMessageHost` (see the **screen-ui** skill). `IngredientsViewModel` (in `screens/ingredients/`) is the reference example.

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
- Write unit-tests.
- After changes, run the **verify** skill.

## Example

See `app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/screens/ingredients/IngredientsViewModel.kt` for a good example of the overall structure and patterns.

Note: the existing ViewModels predate the `StateViewModel` base class and have
not yet been migrated to it — they still declare `innerState`/`uiState`/`State`
by hand with a `private` `State`. New ViewModels should extend `StateViewModel`
as described under *Base class* above.
