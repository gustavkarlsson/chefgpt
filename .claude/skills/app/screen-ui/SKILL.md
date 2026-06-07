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

### Entry point composable

A public `@Composable fun <Screen>Screen(...)` that wires the ViewModel to the UI and nothing else:

```kotlin
@Composable
fun IngredientsScreen(route: Route.Ingredients) {
    val viewModel = koinViewModel<IngredientsViewModel> { parametersOf(route) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Content(uiState)
}
```

- Resolve the ViewModel with `koinViewModel<T>()`. Pass navigation route args via `{ parametersOf(route) }`.
- Collect `uiState` with `collectAsStateWithLifecycle()`.
- Delegate immediately to a private, stateless `Content(uiState)`.

If the ViewModel also exposes a one-shot event `Flow` (see the **view-model** skill's *One-shot events* section), pass that `Flow` to `Content` as an extra parameter and `collect` it where it's consumed (typically in a `LaunchedEffect`). It stays separate from `uiState`:

```kotlin
@Composable
fun ChatScreen(route: Route.Chat) {
    val viewModel = koinViewModel<ChatViewModel> { parametersOf(route) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Content(uiState, viewModel.ingredientChanges)
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
- Lazy lists: stable string keys (`key = { it.id.toString() }`) and `Modifier.animateItem()` on items.
- Every `Icon` and `Image` needs a `contentDescription`.
- Modern Kotlin, immutable data, no more code than necessary.

## Wiring a new screen

When adding a whole new screen (not editing an existing one), three places connect it:
1. `navigation/Route.kt` — add a `@Serializable` route (`data object` or `data class` with nav args).
2. `App.kt` — register it in the `entryProvider` (`entry<Route.X> { key -> XScreen(key) }`).
3. `di/AppModule.kt` — register the ViewModel in `viewModelModule` (`viewModel<XViewModel>()`).

The ViewModel itself is built with the **view-model** skill.

## Example

See `app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/screens/ingredients/IngredientsScreen.kt` for a good example.
