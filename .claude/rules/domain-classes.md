---
description: The four server model layers and the rules for where each may be used.
paths:
  - "server/**/*.kt"
---

# Domain classes

Server models come in four layers (see `docs/adr/0002-server-model-layers.md` and the glossary in
`CONTEXT.md`). A class belongs to exactly one layer; code at each boundary maps to and from the
domain model. Never let one class play two layers at once.

## Domain models

The server's business concepts (`Recipe`, `Ingredient`, `Event`, `UserFacts`), living in their
per-domain package. Pure data classes:

- **No library annotations** — no `@Serializable`, no `@LLMDescription`, no `@Tool`.
- **No library types** — no Koog types, no `Api*` models, no tool models, no DB models. Kotlin
  stdlib types and shared value classes (`RecipeId`, `ImageUrl`, `SpoonacularId`, …) are fine.
- Bare noun names, no prefix or suffix.

If a domain model must be serialized, stored, or shown to the LLM, that is a job for the layer
that owns the concern: an `Api` model for the wire, a `Tool` model for the LLM, a `Stored*` DTO
for a whole-JSON column.

## Tool models

The schema Koog's reflection reads when an agent calls a tool. They live in
`agent/tools/models/` with a `Tool` prefix (`ToolRecipe`, `ToolIngredient`, `ToolUploadedFile`):

- `@Serializable`, with `@LLMDescription` on the class **and on every property** — the LLM sees
  these, not the Kotlin source. A tool model without them is broken for the agent.
- Contain only primitives, kotlin stdlib types, shared value classes, and other tool models —
  never domain or `Api` models.
- Used only by tool methods (`@Tool` parameters and return types), and mapped to and from domain
  models inside the tool handler (extension functions on the domain model, next to the tool model).
- No default values (`koog-tools` and `kotlin-data-classes` rules), except sentinel defaults on
  tool *parameters*, which Koog ignores.

## API models

The wire format, in the shared module with an `Api` prefix. Routes are the only consumers;
they map to and from domain models via `toApi()`/`toDomain()` extension functions collected in a
per-domain mapper file (e.g. `recipes/RecipeMappers.kt`). Domain, tool, and DB layers never
reference `Api*` models or API enums. The API model and its `@SerialName`s are the client-facing
contract — changing one is a snapshot diff, not test upkeep (`json-serialization` rule).

## Database models

Persistence internals that never cross the repository boundary:

- SQLDelight-generated query row types (`se.gustavkarlsson.chefgpt.db.*`).
- Hand-written `Stored*` DTOs for columns that hold a whole-JSON blob (`StoredEvent`). These pin
  the storage format: renaming their fields or `@SerialName`s is a migration, not a refactor.

`Postgres*` repositories map rows and DTOs to and from domain models inline.
