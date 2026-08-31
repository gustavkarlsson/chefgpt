---
description: Baseline style for all Kotlin source in this project.
paths:
  - "**/*.{kt,kts}"
---

# Kotlin style

- Use modern Kotlin language features.
- Don't write more code than necessary.
- Prefer functional patterns and immutable data.
- Comment only when purpose or implementation is unclear.
- Never use fully qualified references — use imports (and typealiases for collisions).

When a constructor parameter may carry a default value, the `kotlin-data-classes` rule is
stricter than "modern Kotlin" suggests — read it before adding one.
