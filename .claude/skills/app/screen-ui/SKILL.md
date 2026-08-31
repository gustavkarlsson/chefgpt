---
name: screen-ui
description: Structure a screen in the `app` module — the `Screen` data class, its `Content()` entry point, ViewModel wiring, and snackbar and one-shot event plumbing. Use when adding a screen or changing how one is wired. A screen is the pairing of a ViewModel (see the view-model skill) and its UI; the composables themselves follow the compose-ui skill.
---

A **screen** is a concept that pairs two halves:
- A **ViewModel** that owns state and exposes a `UiState` — see the **view-model** skill.
- The **UI**: the composables that render that `UiState`.

This skill covers how the screen itself is put together and wired up. How to write the composables
inside it — decomposition, callbacks, `Modifier`, theming, lists, accessibility — is the
**compose-ui** skill.

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

### The private `Content`

`Screen.Content()` delegates immediately to a private `Content(uiState, ...)` in the same file, and
that composable owns the whole screen body. It is stateless and never references the ViewModel — it
takes `uiState` plus any `Flow` the screen collects, which is what keeps the UI previewable.

Everything below that entry point — how the body is broken up, callbacks, `Modifier`, theming,
lists, accessibility — is the **compose-ui** skill.

## Wiring a new screen

When adding a whole new screen (not editing an existing one):
1. Define the `XScreen` data class as above, implementing `Screen` (`@Serializable`, `@SerialName`, nav-arg `val`s, defaulted `id`, `Content()`).
2. `di/AppModule.kt` — register the ViewModel in `viewModelModule` (`viewModel<XViewModel>()`).

That's it. There is no per-screen registration in `App.kt`: `NavDisplay`'s `entryProvider` is generic and renders any screen through `screen.Content()`, keyed by `screen.id.value`. To navigate to the new screen, call `navigator.push(XScreen(...))` from a ViewModel (see the **view-model** skill).

The ViewModel itself is built with the **view-model** skill.

## Example

See `app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/screens/ingredients/IngredientsScreen.kt` for a good example.
