---
name: strings
description: Handle user-facing strings in the `app` module — Compose Resources, `strings.xml` organization, and resolution. Use whenever adding or changing any text the user sees.
---

User-facing strings live in `app/src/commonMain/composeResources/values/strings.xml`.

## No hardcoded strings

Never hardcode user-facing text in code. Put it in `strings.xml` and reference
it by resource ID.

The only exceptions are strings that are *untranslatable* (a product name, a
domain, a URL) or *non-words* (a single punctuation mark, a symbol with no
localized meaning). Mark a name as untranslatable with `translatable="false"`:

```xml
<string name="app_name" translatable="false">ChefGPT</string>
```

## Resolving strings

In a composable, resolve directly with `stringResource` (from
`org.jetbrains.compose.resources`):

```kotlin
Text(stringResource(Res.string.screen_debug_title))
```

In a ViewModel — or any type where resolution is not possible — don't resolve
the string. Put the data the string depends on (or a `StringResource` reference)
in the `UiState`, and resolve it in the UI.

## strings.xml organization

- **Name keys** as `<type-of-context>_<context>_<position-in-context>`:
  - `screen_intro_title` (title of the intro screen)
  - `screen_intro_explore_button` (the explore button)
  - `screen_debug_network_speed_default` (the "Default" option of the network-speed control)

  The last part names the element type: `title`, `description`, `button`, `text`,
  `placeholder`, and so on. The middle part narrows which element within the screen.

- **Group keys** by the context, with a blank line between groups:

  ```xml
  <string name="screen_intro_title">…</string>
  <string name="screen_intro_explore_button">…</string>

  <string name="screen_debug_title">…</string>
  <string name="screen_debug_back">…</string>
  ```

- **Sort keys alphabetically** within each group.
