---
name: add-service
description: Add a new service to the Ktor server. Use when introducing a new interface + implementation(s), wiring into DI, and configuring via the config system.
---

Services live in domain-named packages under `server/src/main/kotlin/se/gustavkarlsson/chefgpt/<domain>/`.

## Before starting

If it's unclear what implementations are needed (e.g. real vs. fake, which external API, database vs. in-memory), **ask the user** before writing any code. Common patterns:

- **Database-backed with in-memory fallback** — `PostgresFoo` + `InMemoryFoo`, selected by whether the database is available in the DI graph.
- **External API with fake** — `RealFoo(apiKey)` + `FakeFoo()`, selected by a config string (see `CreateImageUploaderModule.kt`)
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

## 3. Create a Koin module function

In `server/src/main/kotlin/se/gustavkarlsson/chefgpt/setup/CreateMyServiceModule.kt`:

**If config-driven (type string + optional API keys):**

```kotlin
package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.<domain>.FakeMyService
import se.gustavkarlsson.chefgpt.<domain>.MyService
import se.gustavkarlsson.chefgpt.<domain>.RealMyService

fun createMyServiceModule(config: ApplicationConfig) =
    module {
        single {
            when (val type = config.property("bindings.myService").getString()) {
                "real" -> {
                    val apiKey = config.property("myService.apiKey").getString()
                    RealMyService(apiKey)
                }
                "fake" -> FakeMyService()
                else -> error("Unknown myService type: '$type'. Expected 'real' or 'fake'.")
            }
        } bind MyService::class
    }
```

**If database-backed with in-memory fallback:**

```kotlin
fun createMyServiceModule(config: ApplicationConfig) =
    module {
        single {
            val database = getOrNull<PostgresAccess>()
            if (database != null) PostgresMyService(database) else InMemoryMyService()
        } bind MyService::class
    }
```

---

## 4. Add config entries

In `server/src/main/resources/application.conf`, under the `bindings` block:

```hocon
bindings {
    # ... existing entries ...
    myService = "real" # or "fake"
    myService = ${?MY_SERVICE}
}

myService {
    apiKey = ${?MY_SERVICE_API_KEY}
}
```

In `application_dev_template.conf` (at the repo root, used to set up new dev environments), set safe defaults:

```hocon
bindings {
    myService = "fake"
}
```

---

## 5. Wire into InstallKoin.kt

In `server/src/main/kotlin/se/gustavkarlsson/chefgpt/plugins/InstallKoin.kt`, add the module:

```kotlin
fun Application.installKoin() {
    val config = environment.config
    install(Koin) {
        slf4jLogger()
        modules(
            // ... existing modules ...
            createMyServiceModule(config),
        )
        createEagerInstances()
    }
}
```

---

## Key conventions

- If a service holds resources (HTTP clients, DB connections), implement `AutoCloseable`
- Use `config.propertyOrNull("...")?.getString()` for optional config values
- Use `config.property("...").getString()` (throws) for required config values
- Fake implementations are for local dev (`application_dev_template.conf`) and tests
- Production defaults go in `application.conf`
- Bind by interface using `} bind MyService::class` so Koin resolves the interface type
