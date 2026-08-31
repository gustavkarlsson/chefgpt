---
description: How JSON is configured in this project and what that implies for serializable models.
paths:
  - "**/*.kt"
---

# JSON serialization

## Getting a `Json`

Always `chefGptJson(strict = ...)`, or the injected `Json` on the server. Never a `Json { }` block,
Ktor's `json()` or a bare `Json`, in main or test source.

`strict` has no default, so pick one:

- **`true`** — reject anything the model doesn't describe exactly. Development mode and tests.
- **`false`** — accept what we can make sense of and ignore the rest. Input we don't control: live
  traffic, stored records, third-party APIs, LLM output.

`prettyPrint` is separate and off unless asked for.

## Defaults

Don't default a property on a `@Serializable` type.

The exception is a **new required** property, where the default lets older serialized instances still
be read. Comment it as such, and drop it once nothing needs it.

Koog tool schemas ignore Kotlin defaults entirely — use a sentinel value to make a `@Tool` parameter
optional.

## Schema changes

A changed response body shows up as a diff in `server/src/test/snapshots/`. Treat it as a
client-facing change, not test upkeep.
