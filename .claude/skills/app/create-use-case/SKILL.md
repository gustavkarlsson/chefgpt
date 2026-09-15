---
name: create-use-case
description: Write a use case in the `app` module — a small piece of business logic that sits between a ViewModel and a repository. Use when business logic is too big for a ViewModel, or when a ViewModel would otherwise call a repository directly.
---

Use cases are the layer this repo's `TODO Introduce use-case` markers call for: business
logic that should live between a ViewModel and a repository. New logic goes in a use
case; existing ViewModels that call repositories directly are migrated opportunistically
when they grow past simple delegation — don't bulk-churn working code.

## Shape

A use case is a verb-first `fun interface` plus an implementation prefixed by what it's
based on:

```kotlin
package se.gustavkarlsson.chefgpt.chats

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError

fun interface GetChat {
    suspend operator fun invoke(id: ChatId): Result<Chat, ClientError>
}

class ApiGetChat(
    private val repository: ChatRepository,
) : GetChat {
    override suspend fun invoke(id: ChatId): Result<Chat, ClientError> =
        repository.get(id)
}
```

- Name the interface `VerbDomain` (`GetChat`, `SaveRecipe`, `RemoveIngredient`); prefix
  the implementation with what it's based on (`ApiGetChat`).

## Location

Use cases live alongside the repository they call, in
`app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/<domain>/` (e.g. `chats/`,
`recipes/`, `sessions/`). Group several small ones in `FooUseCases.kt`, or one per file.

## Return types

- One-shot work: `Result<T, ClientError>` — `ClientError` from `ChefGptClient`.
- Infallible work: `T` or `Unit`.
- Streams: `Flow<T>`.

Always `com.github.michaelbull.result.Result`, never `kotlin.Result`. Handle errors with
kotlin-result rather than `try`/`catch`.

## Wiring

Register in `di/AppModule.kt`'s `singletonModule`, bound by interface:

```kotlin
single<ApiGetChat>() bind GetChat::class
```

ViewModels inject the interface and call it like any other collaborator — see the
**view-model** skill for the ViewModel side.

After any change, run the **verify** skill.
