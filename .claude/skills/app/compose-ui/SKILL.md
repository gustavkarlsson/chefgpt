---
name: compose-ui
description: Conventions for writing Compose composables in the `app` module — decomposition, statelessness, callbacks, `Modifier`, Material3 theming, lists and accessibility. Use whenever writing or changing any composable, whether it belongs to a screen or lives on its own (theme, snackbar host, shared widgets).
---

These conventions apply to **every** composable in `app`, not just screens. For how a screen is
assembled (the `Screen` data class, ViewModel wiring, snackbar and event plumbing), use the
**screen-ui** skill; for the state it renders, the **view-model** skill.

## Decomposition

- Composables are **private** unless something outside the file renders them, and **stateless**:
  they take the data they render plus callbacks, and never reference a ViewModel.
- Break UI into small composables, each taking only the slice of state it renders. Mirror the
  structure of the state object.
- A child takes plain data + callbacks rather than the whole state object when that keeps it
  focused.
- Branch mutually exclusive states with `when` over a `sealed interface` and render a composable
  per case (e.g. `Loading`, `Loaded`, `Error`).

## Callbacks

- Invoke callbacks straight off the state object. Never put business logic in a composable.
- A nullable callback means "disabled". Drive the control's enabled state off it and grey out /
  swap the control accordingly — don't add a separate `enabled` flag:
  ```kotlin
  IconButton(onClick = { input.onClickAdd?.invoke() }, enabled = input.onClickAdd != null) { ... }
  ```
- Keep transformations (sorting, filtering, formatting) out of the UI. The composable renders
  ready-made data prepared by the ViewModel.

## Modifier

- Every composable takes a `Modifier` parameter.
- It is the first optional parameter, placed immediately after any required arguments (excluding a
  trailing content lambda), and always defaulted to `Modifier`.
- When passing a modifier as an argument, place it first in the named argument list — despite the
  parameter not being first.

## Theming

Material3 only. Use `MaterialTheme.typography` / `MaterialTheme.colorScheme`; never hard-coded
styles or colors.

## Lists

Lazy lists take stable string keys on items (`key = { it.id.toString() }`). Don't animate items
unless the user explicitly asks for it.

## Accessibility

Every `Icon` and `Image` needs a `contentDescription`.
