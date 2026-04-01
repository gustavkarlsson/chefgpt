---
name: add-service
description: Add a new service to the Ktor server. Use when introducing a new interface + implementation(s), wiring into DI, and configuring via the config system.
---

Services live in domain-named packages under `server/src/main/kotlin/se/gustavkarlsson/chefgpt/<domain>/`.

## Before starting

If it's unclear what implementations are needed (e.g. real vs. fake, which external API, database vs. in-memory), **ask the user** before writing any code. Common patterns:

- **Database-backed with in-memory fallback** — `PostgresFoo` + `InMemoryFoo`, selected by whether `database` is non-null (see `CreateChatRepository.kt`)
- **External API with fake** — `RealFoo(apiKey)` + `FakeFoo()`, selected by a config string (see `CreateImageUploader.kt`)
- **Single implementation** — no config switch needed, just wire directly

---

## 1. Create the interface

In `server/src/main/kotlin/se/gustavkarlsson/chefgpt/<domain>/MyService.kt`:

```kotlin
package se.gustavkarlsson.chefgpt.<domain>

interface MyService {
    suspend fun doSomething(input: String): String
}
```

Always create an interface even for services with a single implementation.

---

## 2. Create implementation(s)

Prefix the interface with whatever the implementation is based on. E.g. Postgres, Api, File, InMemory, etc.

Real implementation in `<domain>/ApiMyService.kt`:

```kotlin
class ApiMyService(private val apiKey: String) : MyService, AutoCloseable {
    private val client = HttpClient(CIO)

    override suspend fun doSomething(input: String): String {
        // ...
    }

    override fun close() = client.close()
}
```

Fake/in-memory implementation in `<domain>/FakeMyService.kt`:

```kotlin
class FakeMyService : MyService {
    override suspend fun doSomething(input: String): String = "fake result"
}
```

---

## 3. Create a setup function

In `server/src/main/kotlin/se/gustavkarlsson/chefgpt/setup/CreateMyService.kt`:

**If config-driven (type string + optional API keys):**

```kotlin
package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import se.gustavkarlsson.chefgpt.<domain>.FakeMyService
import se.gustavkarlsson.chefgpt.<domain>.MyService
import se.gustavkarlsson.chefgpt.<domain>.RealMyService

fun createMyService(type: String, config: ApplicationConfig): MyService =
    when (type) {
        "real" -> {
            val apiKey = config.property("chefgpt.myService.apiKey").getString()
            RealMyService(apiKey)
        }
        "fake" -> FakeMyService()
        else -> error("Unknown myService type: '$type'. Expected 'real' or 'fake'.")
    }
```

## 4. Add config entries

In `server/src/main/resources/application.conf`, under the `chefgpt` block:

```hocon
chefgpt {
    # ... existing entries ...
    myService = "real" # or "fake"
    myService = ${?MY_SERVICE}
    myServiceApiKey = ${?MY_SERVICE_API_KEY}
}
```

In `application_dev_template.conf` (used to set up new dev environments), set safe defaults:

```hocon
chefgpt {
    myService = "fake"
}
```

---

## 5. Wire into Application.kt

In `server/src/main/kotlin/se/gustavkarlsson/chefgpt/Application.kt`:

```kotlin
val myService = createMyService(
    type = config.property("chefgpt.myService").getString(),
    config = config,
)
```

Then pass it to `installDependencies(...)`.

---

## 6. Register in InstallDependencies.kt

In `server/src/main/kotlin/se/gustavkarlsson/chefgpt/plugins/InstallDependencies.kt`:

Add the parameter and register it:

```kotlin
fun Application.installDependencies(
    // ... existing params ...
    myService: MyService,
) {
    dependencies {
        // ... existing ...
        provide<MyService> { myService }
    }
}
```

Use `provide<Interface> { instance }` (with explicit type) when registering by interface — this is required for injection to resolve the interface type rather than the concrete class.

## Key conventions

- If a service holds resources (HTTP clients, DB connections), implement `AutoCloseable`
- Use `config.propertyOrNull("...")?.getString()` for optional config values
- Use `config.property("...").getString()` (throws) for required config values
- Fake implementations are for local dev (`application_dev_template.conf`) and tests
- Production defaults go in `application.conf`
