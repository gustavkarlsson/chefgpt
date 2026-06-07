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
    val uiState by viewModel.uiState.collectAsState()
    Content(uiState)
}
```

- Resolve the ViewModel with `koinViewModel<T>()`. Pass navigation route args via `{ parametersOf(route) }`.
- Collect `uiState` with `collectAsState()`.
- Delegate immediately to a private, stateless `Content(uiState)`.

### Content and child composables

- `Content` and all child composables are `private` and **stateless**: they take a `UiState` (or a slice of it) and never reference the ViewModel. This keeps the UI previewable and testable.
- Break the UI into small private composables, each taking only the slice of state it renders. Mirror the `UiState` hierarchy.
- A child takes plain data + callbacks, not the whole `UiState`, when that keeps it focused (see `ChatSidebar`, `LoggedOutContent`).
- For `sealed interface` UiState variants, branch with `when` and render a composable per case (see `StartScreen`'s `LoggedOut`/`LoggedIn`).

## Callbacks

- Invoke callbacks straight from `UiState`; never put business logic in the UI.
- A nullable callback means "disabled". Drive the control's enabled state off it and grey out / swap the control accordingly — don't add separate `enabled` flags:
  ```kotlin
  IconButton(onClick = { input.onClickAdd?.invoke() }, enabled = input.onClickAdd != null) { ... }
  ```
- Keep transformations (sorting, filtering, formatting) in the ViewModel. The UI renders ready-made data.

## Conventions

- Material3 only. Use `MaterialTheme.typography` / `MaterialTheme.colorScheme`, never hard-coded styles or colors.
- `Modifier` is the last parameter, defaulting to `Modifier`. Apply caller modifiers first.
- Lazy lists: stable string keys (`key = { it.id.toString() }`) and `Modifier.animateItem()` on items.
- Every `Icon` needs a `contentDescription`.
- Modern Kotlin, immutable data, no more code than necessary. Use imports, never fully qualified references.

## Wiring a new screen

When adding a whole new screen (not editing an existing one), three places connect it:
1. `navigation/Route.kt` — add a `@Serializable` route (`data object` or `data class` with nav args).
2. `App.kt` — register it in the `entryProvider` (`entry<Route.X> { key -> XScreen(key) }`).
3. `di/AppModule.kt` — register the ViewModel in `viewModelModule` (`viewModel<XViewModel>()`).

The ViewModel itself is built with the **view-model** skill.

## Completing

- Write unit-tests using the unit-test skill where the UI has testable logic.
- After changes, run the **verify** skill.

## Example

See `app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/screens/ingredients/IngredientsScreen.kt` for a good example.
