---
description: How Koog tools are written, annotated, and kept visible to agents.
paths:
  - "server/**/*.kt"
---

# Koog tools

An agent reaches a tool through a `ToolSet` implementation whose `@Tool`-annotated methods are
collected by reflection. The LLM sees each method's `@LLMDescription` and each parameter's
`@LLMDescription` — not the Kotlin source.

## The tool class

- Implement `ToolSet`.
- Never `private`: Koog instantiates it and reads its methods reflectively, which cannot see
  `private` declarations. When a tool isn't meant to be public API, keep it visible and mark it
  `@VisibleForTesting` (`org.jetbrains.annotations.VisibleForTesting`) instead of `private`.
- Its `name` defaults to the class's JVM name. Annotate the class with `@LLMDescription` only to
  give the toolset a custom name.

## Tool methods

- Mark each with `@Tool`. Methods are `suspend` and never `private`.
- Describe what it does with `@LLMDescription(...)` — the description the LLM reads.
- `@Tool(customName = "...")` renames a tool; omit it to use the function name.

## Parameters

- Describe every parameter with `@LLMDescription(...)`: what it means, and an example where helpful.
- Kotlin default values are ignored when Koog builds the tool's schema, so a parameter cannot be
  truly optional. To make one optional, give it a sentinel default (`0`, `""`, `emptyList()`) and
  treat that sentinel as "absent" in the method body.
