---
description: Baseline style for all Kotlin source in this project.
paths:
  - "**/*.{kt,kts}"
---

# Kotlin style

- Use modern Kotlin language features.
- Don't write more code than necessary.
- Write idiomatic Kotlin.
- Comment only when purpose or implementation is unclear.
- Never use fully qualified references — use imports (and typealiases for collisions).
- Use braces in `when` branches when the expression returns `Unit`.
- Use braces in `if` statements unless the expression contains no logic and fits on the same line.
- Don't use expression function bodies for functions returning `Unit`.
- Every public declaration carries an explicit type — public functions with a
  non-`Unit` return type and public properties spell out their type; locals and
  `private` members may be implicit.
- Prefer immutable data: `val` over `var`, and read-only collection types
  (`List`/`Set`/`Map`) over `Mutable*` in public signatures.
- Prefer a plain loop or `if` over a functional chain for side effects. Use
  `map`/`filter`/`associateBy`/`sumOf` to *transform* data; `forEach` for effect
  is a `for` loop with extra ceremony.
- Use scope functions to avoid repeating a receiver, not to build a pipeline;
  name the lambda parameter instead of `it` when it would otherwise be ambiguous.

When a constructor parameter may carry a default value, the `kotlin-data-classes` rule is
stricter than "modern Kotlin" suggests — read it before adding one.
