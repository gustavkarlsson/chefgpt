---
name: screen-ui
description: Create or modify the Compose UI of a screen in the `app` module. Use this skill when building or changing the composables that render a screen. A screen is the pairing of a ViewModel (see the view-model skill) and its UI; this skill covers the UI half only.
---

A **screen** is a concept that pairs two halves:
- A **ViewModel** that owns state and exposes a `UiState` — see the **view-model** skill.
- The **UI**: the composables that render that `UiState` — covered here.

UI lives alongside its ViewModel in `app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/screens/<screen>/`, in a file named after the screen:
- `UserViewModel` -> `UserScreen.kt`
- `SettingsViewModel` -> `SettingsScreen.kt`

## Structure

### Screen data class

A screen is a `@Serializable data class` implementing the `Screen` interface (`navigation/Screen.kt`). It holds the navigation args as properties, carries a stable `id`, and exposes a `Content()` composable that wires the ViewModel to the UI and nothing else:

```kotlin
@Serializable
@SerialName("ingredients")
data class IngredientsScreen(
    val sessionId: SessionId,
    override val id: Id = Id.new(),
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<IngredientsViewModel> { parametersOf(this) }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Content(uiState)
    }
}
```

- `@Serializable` + `@SerialName("<screen>")` — the screen is persisted in the back stack, so the serial name must be stable and unique.
- Declare navigation args as `val` properties (e.g. `sessionId`). A screen with no args is still a `data class` (not a `data object`) because of the `id`.
- `override val id: Id = Id.new()` — last, always defaulted. `Id` distinguishes two back-stack entries of the same screen type; never set it manually.
- Resolve the ViewModel with `koinViewModel<T>()`. Pass the screen itself to the ViewModel via `{ parametersOf(this) }` — the ViewModel reads its nav args off the screen (see the **view-model** skill).
- Collect `uiState` with `collectAsStateWithLifecycle()`.
- Delegate immediately to a private, stateless `Content(uiState)`.

If the ViewModel also exposes a one-shot event `Flow` (see the **view-model** skill's *One-shot events* section), pass that `Flow` to `Content` as an extra parameter and `collect` it where it's consumed (typically in a `LaunchedEffect`). It stays separate from `uiState`:

```kotlin
@Serializable
@SerialName("chat")
data class ChatScreen(
    val sessionId: SessionId,
    val chatId: ChatId,
    override val id: Id = Id.new(),
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<ChatViewModel> { parametersOf(this) }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Content(uiState, viewModel.ingredientChanges)
    }
}
```

### Snackbars

Every ViewModel exposes `snackbarMessages: Flow<SnackbarMessage>` from the base `StateViewModel` (see the **view-model** skill's *Snackbars* section). If the screen surfaces snackbars, pass that `Flow` into `Content`, turn it into a host state with `rememberSnackbarHostState(...)`, and render it through the `Scaffold`'s `snackbarHost` with `SnackbarMessageHost` (all from `se.gustavkarlsson.chefgpt.snackbar`):

```kotlin
@Serializable
@SerialName("ingredients")
data class IngredientsScreen(
    val sessionId: SessionId,
    override val id: Id = Id.new(),
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<IngredientsViewModel> { parametersOf(this) }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Content(uiState, viewModel.snackbarMessages)
    }
}

@Composable
private fun Content(
    uiState: UiState,
    snackbarMessages: Flow<SnackbarMessage>,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = rememberSnackbarHostState(snackbarMessages)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarMessageHost(snackbarHostState) },
        // ...
    ) { /* ... */ }
}
```

### Content and child composables

- `Content` and all child composables are `private` and **stateless**: they take a `UiState` (or a slice of it) and never reference the ViewModel. This keeps the UI previewable and testable.
- Break the UI into small private composables, each taking only the slice of state it renders. Mirror the `UiState` hierarchy.
- A child takes plain data + callbacks, not the whole `UiState`, when that keeps it focused.
- For `sealed interface` UiState variants, branch with `when` and render a composable per case (e.g. `Loading`, `Loaded`, `Error`).

## Callbacks

- Invoke callbacks straight from `UiState`; never put business logic in the UI.
- A nullable callback means "disabled". Drive the control's enabled state off it and grey out / swap the control accordingly — don't add separate `enabled` flags:
  ```kotlin
  IconButton(onClick = { input.onClickAdd?.invoke() }, enabled = input.onClickAdd != null) { ... }
  ```
- Keep transformations (sorting, filtering, formatting) in the ViewModel. The UI renders ready-made data.

## Conventions

- Material3 only. Use `MaterialTheme.typography` / `MaterialTheme.colorScheme`, never hard-coded styles or colors.
- All composables should have a `Modifier` parameter.
- `Modifier` is first optional parameter, placed immediately after any required arguments (excluding any trailing content lambda). It's always defaulted to `Modifier` which is the empty modifier.
- When setting a modifier as an argument, always place it first in the named argument list (despite the parameter not being first).
- Lazy lists: stable string keys (`key = { it.id.toString() }`) on items. Don't animate items unless the user explicitly asks for it.
- Every `Icon` and `Image` needs a `contentDescription`.
- Modern Kotlin, immutable data, no more code than necessary.

## Wiring a new screen

When adding a whole new screen (not editing an existing one):
1. Define the `XScreen` data class as above, implementing `Screen` (`@Serializable`, `@SerialName`, nav-arg `val`s, defaulted `id`, `Content()`).
2. `di/AppModule.kt` — register the ViewModel in `viewModelModule` (`viewModel<XViewModel>()`).

That's it. There is no per-screen registration in `App.kt`: `NavDisplay`'s `entryProvider` is generic and renders any screen through `screen.Content()`, keyed by `screen.id.value`. To navigate to the new screen, call `navigator.push(XScreen(...))` from a ViewModel (see the **view-model** skill).

The ViewModel itself is built with the **view-model** skill.

## Example

See `app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/screens/ingredients/IngredientsScreen.kt` for a good example.
