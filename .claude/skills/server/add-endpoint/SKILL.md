---
name: add-endpoint
description: Add a new HTTP endpoint to the Ktor server. Use when implementing any new server route, whether it needs a new repository/service or just a new handler on an existing one.
---

Each route lives in its own file under `server/src/main/kotlin/se/gustavkarlsson/chefgpt/routes/`.

## 1. Create API models if needed

Define request/response models in `shared/src/commonMain/.../api/` so mobile clients can reuse them:

```kotlin
@Serializable
@SerialName("api-my-thing")
data class ApiMyThing(
    val id: MyThingId,
    val name: String,
)
```

Use inline value classes for IDs and other single-value classes. Use a custom serializer to serialize it to a primitive (follow the pattern in `ChatId.kt`).

## 2. Create the route file

In `server/src/main/kotlin/se/gustavkarlsson/chefgpt/routes/MyThingRoute.kt`:

```kotlin
package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.toApi

fun Route.myThingRoute() {
    get("/my-things") {
        val myThingRepository = get<MyThingRepository>()
        val session = call.requireSession()
        val things = myThingRepository.getAll(session.user.id)
        call.respond(HttpStatusCode.OK, things.map { it.toApi() })
    }
}
```

For error cases, use the `ResponseData` response model.

## 3. Register in InstallRouting.kt

In `server/src/main/kotlin/se/gustavkarlsson/chefgpt/plugins/InstallRouting.kt`, add the route function to the authed or unauthed scope.

## 4. Snapshot test

Add a snapshot test — see /snapshot-test for conventions.

## Key conventions

- Domain-to-API conversions belong in extension functions (e.g., `fun MyThing.toApi(): ApiMyThing`)
- DI is Koin — use `get<MyService>()` inside route handlers (imports `org.koin.ktor.ext.get`)
- Routes inside `authenticate { }` are protected; routes outside are public
