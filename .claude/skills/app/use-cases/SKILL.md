---
name: use-cases
description: Create, edit, or use a use case in the `app` module — a named, injectable `fun interface` that a ViewModel depends on. Use when a ViewModel needs business logic, or when it would otherwise call a repository, client, or manager directly.
---

Use cases are the app's business-logic layer. A ViewModel depends on use-case
interfaces, never on repositories, clients, or managers (the only exceptions are
`Navigator` and config models like `DeviceConfig`). See the **view-model** skill for
the ViewModel side.

## What a use case is

A use case is a verb-first `fun interface` with a single `operator fun invoke`
(usually `suspend`). It is named, injectable, and fakeable in tests with a plain
lambda.

The rare use case whose `invoke` is generic (e.g. `AwaitJob`) is a plain
`interface` instead — a `fun interface` cannot have a generic method.

## Shape

```kotlin
package se.gustavkarlsson.chefgpt.recipes

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface GetRecipe {
    suspend operator fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError>
}

class HttpGetRecipe(
    private val repository: RecipeRepository,
) : GetRecipe {
    override suspend fun invoke(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError> = repository.get(sessionId, recipeId)
}
```

## Naming

- Interface: `VerbDomain` — an action plus a domain noun (`GetRecipe`,
  `StreamChats`, `SetRecipeFavorite`). One operation per use case.
- Implementation: prefix with what backs it — `Http` for remote work, `Real` for
  everything else (`HttpGetRecipe`, `RealShowSnackbar`). Repositories follow the
  same `Http` prefix rule (`HttpRecipeRepository`).

## Location

One file per use case, holding the `fun interface` and its single default
implementation together. Place it in a `usecases` sub-package of the domain it
belongs to:

```
app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/<domain>/usecases/<VerbDomain>.kt
```

## Statelessness

Use cases are always stateless. Any state or cache lives in the injected
collaborator (repository, `JobManager`, resolver, `Settings`), never in the use
case.

## Composition

A use case implementation may inject other use cases (interfaces), not just
repositories or clients.

## Return types

- One-shot work: `Result<T, E>` — `E` is the use case's own error type;
  `ClientError` (from `ChefGptClient`) is the common case. Always
  `com.github.michaelbull.result.Result`, never `kotlin.Result`.
- Infallible work: `T` or `Unit`.
- Streams: `Flow<T>`.

## Wiring

Register in `di/AppModule.kt`'s `singletonModule`, bound by interface:

```kotlin
single<HttpGetRecipe>() bind GetRecipe::class
```

ViewModels inject the interface and call it like any other collaborator — see the
**view-model** skill for the ViewModel side.

## Editing

When a use case's signature changes, update its interface, implementation, and the
call sites (ViewModels, and any other use cases that inject it) together. Keep the
`fun interface` at one abstract method; if a second operation appears, split it into
its own use case rather than adding a method.

## Testing

Fake a use case in unit tests with a lambda, since a `fun interface` is a single
function:

```kotlin
val getRecipe: GetRecipe = GetRecipe { _, _ -> Ok(recipe) }
```

Only reach for a class-based fake when the test needs to record or assert multiple
calls.

After any change, run the **verify** skill.
