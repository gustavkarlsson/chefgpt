---
name: add-endpoint
description: Add a new HTTP endpoint to the Ktor server. Use when implementing any new server route, whether it needs a new repository/service or just a new handler on an existing one.
---

All routes live in a single file (`Routes.kt`).

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

## 2. Route handler in Routes.kt

Inject dependencies with `by application.dependencies`. Use `call.requireSession()` for authenticated endpoints:

```kotlin
authenticate {
    route("/my-things") {
        get {
            val myThingRepository: MyThingRepository by application.dependencies
            val session = call.requireSession()
            val things = myThingRepository.getAll(session.user.id)
            call.respond(HttpStatusCode.OK, things.map { it.toApi() })
        }

        post {
            val myThingRepository: MyThingRepository by application.dependencies
            val session = call.requireSession()
            val request = call.receive<ApiCreateMyThing>()
            val thing = myThingRepository.create(session.user.id, request.name)
            call.respond(HttpStatusCode.Created, thing.toApi())
        }
    }
}
```

For error cases, use the `ResponseData` response model

## 3. Snapshot test

Add a snapshot test — see /snapshot-test for conventions.

## Key conventions

- Domain-to-API conversions belong in extension functions (e.g., `fun MyThing.toApi(): ApiMyThing`)
- The DI system is Ktor's built-in `dependencies` plugin, not Koin — use `by application.dependencies` at the call site
- Routes inside `authenticate { }` are protected; routes outside are public
