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

When a constructor parameter may carry a default value, the `kotlin-data-classes` rule is
stricter than "modern Kotlin" suggests — read it before adding one.
