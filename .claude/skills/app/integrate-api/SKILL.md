---
name: integrate-api
description: Add or change a client API call against the ChefGPT server — a new request or SSE stream in ChefGptClient, or a shared Api* model. Use when wiring the app to a backend endpoint. For the server side of the endpoint, use the add-endpoint skill.
---

All client networking goes through one gateway class:
`app/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/ChefGptClient.kt` — a Ktor
`HttpClient(CIO)` with `ContentNegotiation` (kotlinx.serialization JSON), `SSE`, and
`Logging`. The server side of any endpoint is the **add-endpoint** skill; this skill
covers the client call only.

## Request/response (one-shot)

One-shot calls use the `request(...)` helper, which turns every failure — including
connection problems — into a `ClientError` instead of a thrown exception:

```kotlin
suspend fun getRecipe(
    sessionId: SessionId,
    recipeId: RecipeId,
): Result<ApiRecipe, ClientError> =
    request(
        send = { baseUrl ->
            get("$baseUrl/recipes/$recipeId") {
                sessionIdHeader(sessionId)
                accept(ContentType.Application.Json)
            }
        },
        readSafe = { body<ApiRecipe>() },
    )
```

- `send` builds the request against `baseUrl` (resolved from `Settings`). Use the
  `io.ktor.client.request.*` extension verbs (`get`/`post`/`patch`/`delete`).
- `readSafe` parses the success body; use `body<ApiX>()`, `bodyAsText()`, or a header.
- Authed endpoints set the session header via `sessionIdHeader(sessionId)`.
- Return `com.github.michaelbull.result.Result<T, ClientError>`. Never throw, never
  return `kotlin.Result` — see the error section.

## Streaming (server-sent events)

Streams use `channelFlow` + the shared `sseTyped` helper:

```kotlin
fun listenToChats(sessionId: SessionId): Flow<List<ApiChat>> =
    channelFlow {
        val baseUrl = settings.getBaseUrl()
        httpClient.sseTyped<List<ApiChat>>(
            json = json,
            eventType = "chats",
            request = {
                url("$baseUrl/chats")
                sessionIdHeader(sessionId)
            },
        ) { _, incoming ->
            incoming.collect(::send)
        }
    }
```

`eventType` is the SSE event name the server emits. `sseTyped` lives in
`shared/src/commonMain/kotlin/se/gustavkarlsson/chefgpt/util/KtorClient.kt`.

## Shared API models

`@Serializable` `Api*` models live in `shared/src/commonMain/.../api/` so the client and
server reuse them. Follow the **add-endpoint** conventions: `@SerialName` where the wire
name differs, never defaulted properties, and IDs as inline value classes with a custom
serializer (see the existing `ChatId`). No `Instant`/`Duration`/other non-primitive
fields — keep `Api*` fields primitive (String/Int/Long/Boolean/Double/enums/nested `Api*`).

## Errors

`ChefGptClient` defines `sealed interface ClientError`:

```kotlin
sealed interface ClientError {
    data class Http(val status: HttpStatusCode, val errorBody: ApiError?) : ClientError
    data object Other : ClientError
}
```

- `Http` — the server responded with a non-success status; `errorBody` is the parsed
  `ApiError` when the body is one.
- `Other` — the request never produced a usable response (e.g. connection failure).

Repositories and use cases handle these with kotlin-result — never with exceptions.

## JSON

Always `chefGptJson(strict = ...)` (or the injected `Json`), never a bare `Json {}`,
per the **json-serialization** rule. `ChefGptClient` already builds one from
`Settings`; reuse it.

## Layering

Repositories (`ChatRepository`, `RecipeRepository`, `SessionRepository`) wrap
`ChefGptClient` and expose domain types. Use cases call repositories, not
`ChefGptClient` directly.

After any change, run the **verify** skill, and add unit tests for any new mapping or
error branch (per the **review** skill's test-coverage expectations).
