---
description: When to give a data class constructor parameter a default value.
paths:
  - "**/*.kt"
---

# Data class defaults

**Never give a property a default value unless there is a very good reason.** Saving keystrokes at
the call site is not one. A required parameter forces every construction site to say what it means,
and turns a newly added field into a compile error rather than a silent `null`.

A good reason is structural — the default expresses something the type could not otherwise say. The
two in this codebase:

- **Screen implementations**: `override val id: Id = Id.new()`. Navigating somewhere should mint a
  fresh identity, so the default *is* the behaviour.
- **Update/modify models** such as `RecipeUpdate`: every field defaults to `null`, meaning "leave
  unchanged". Absence carries meaning, and a caller patching one field genuinely omits the rest.
  Document the meaning in a KDoc on the class.

A `@Serializable` type that crosses the network or disk is stricter still — never default there,
except on a **new required** property, where the default stands in for the value older serialized
instances were written without. The `json-serialization` rule covers both.

## Never default for tests

Do not add a default to a production type to make a test shorter. Write a `private fun` fixture in the
test file that names every field — see `carbonara()` in `InMemoryRecipeStoreTest.kt`. A test asserting
against a defaulted constructor hides what it is actually claiming.
