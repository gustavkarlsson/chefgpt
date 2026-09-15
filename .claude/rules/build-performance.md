---
description: Keep Gradle caching (task, build, and configuration cache) working and fast. Triggered on Gradle build files, gradle.properties, the version catalog, the wrapper, and the CI workflows.
paths:
  - "**/*.gradle.kts"
  - "gradle.properties"
  - "gradle/libs.versions.toml"
  - "gradle/wrapper/gradle-wrapper.properties"
  - ".github/workflows/verify.yml"
  - ".github/workflows/cache-cleanup.yml"
---

# Build performance

Gradle has three layers of caching that keep builds fast. Each is enabled in
`gradle.properties`, and each has things that silently break it. When touching
any build file, keep all three working.

## The three layers

| Layer | Enabled by | What it skips |
|---|---|---|
| Task caching | up-to-date checks (`@Input`/`@Output`) | re-running a task whose inputs are unchanged |
| Build cache | `org.gradle.caching=true` | re-running a task whose outputs are already cached — across builds and machines |
| Configuration cache | `org.gradle.configuration-cache=true` | the whole configuration phase |

`gradle.properties` must have available caches turned on (unless default).

Don't downgrade or delete any of them without a reason.

## gradle.properties

The rest of `gradle.properties` configures the JVMs that run the build and how work is
parallelized:

- **JVM settings per process** — the Gradle daemon and the Kotlin compiler daemon are
  separate JVMs, each sized independently:
  - `org.gradle.jvmargs` — the Gradle daemon (`-Xmx4096M`, a metaspace cap, a reserved
    code cache, and `-Dfile.encoding=UTF-8`).
  - `kotlin.daemon.jvmargs` — the Kotlin compiler daemon (`-Xmx4096M`, a metaspace cap,
    and a reserved code cache).
  Tune each JVM individually, from that daemon's measured usage — not in lockstep.
- **Parallel execution** — `org.gradle.parallel=true` lets Gradle run independent tasks
  from different modules at the same time. Set `org.gradle.tooling.parallel=true` as well,
  so the Tooling API (used by IDEs for sync and model building) works in parallel. Don't
  disable either without a reason.
- **Caching flags** — `org.gradle.caching=true` and `org.gradle.configuration-cache=true`
  are described under *The three layers* above.

## Keeping task and build caching working

A task is cacheable only when Gradle can prove its outputs are a pure function
of its inputs:

- **Declare inputs and outputs.** A task that touches files without declaring
  them (`@Input`, `@InputFiles`, `@OutputFile`, `@OutputDirectory`, `@Internal`,
  …) is never up-to-date and pollutes the build cache.
- **No absolute paths in inputs or outputs.** Cached tasks must be relocatable;
  a path containing the build directory or a machine-specific location defeats
  the cache. Use relative paths or `Project.layout`.
- **No timestamps, environment, or system properties as inputs.** Capture only
  what affects the output. `System.currentTimeMillis()`, `System.getenv()`, and
  host-specific values at execution time make the output non-reproducible.
- **Reproducible outputs.** Sort where order is not semantic; don't embed
  timestamps or absolute paths in generated files.
- **Don't disable caching.** `@DisableCachingByDefault`, `outputs.cacheIf { false }`,
  or `doNotCacheIf(...)` are for tasks whose outputs are genuinely not worth
  caching — not a workaround for a misconfigured task.
- **No machine- or run-specific inputs.** Before adding a `buildConfigField`,
  manifest placeholder, or task input, ask whether two machines compute the same
  value. Preference order: drop it; collapse to a fixed constant on CI via the
  `CI` env var read through a `Provider`; or keep it out of anything upstream of
  an expensive task.

## Keeping the configuration cache working

The configuration cache serializes the result of the configuration phase and
replays it on the next build. Anything read at *configuration time* becomes an
input and, if it changes often or isn't serializable, breaks the cache:

- **Keep configuration pure.** Don't read the environment, system properties,
  the clock, or mutable external state at configuration time; read them inside a
  task action instead.
- **No `Project` references at execution time.** Capture values in configuration
  and pass plain values into task actions; holding `project` in a lambda that
  runs later makes it unserializable.
- **Use provider chains.** Prefer `Provider`/`Property`/`ConfigurableFileCollection`
  over eager `String`/`File` so Gradle can track and serialize values lazily.
- **Use the provider APIs for external state.** Wrap external reads (network,
  filesystem, git, subprocess) in a `ValueSource`. Read env and Gradle properties
  via `providers.environmentVariable(...)` / `providers.gradleProperty(...)` — never
  bare `System.getenv()` / `System.getProperty()`. Never call `.get()` on a
  `Provider` during configuration — chain with `map`/`flatMap`/`orElse`.
- **Register tasks lazily.** Prefer `tasks.register`/`configureEach`; avoid
  `create`, `getByName`, and bare `tasks.withType<T> { }`.
- **Declare the inputs a task uses.** A `doLast { }` that reads a file it never
  declared breaks both the configuration cache and the task cache.
- **Watch the warnings.** A "Configuration cache problems found" warning means
  Gradle fell back or will refuse to cache. Treat it as a bug to fix, not noise.

## Incremental compilation

Compilation is incremental only while the code everything depends on doesn't
change: prefer compiler plugins (Compose compiler, Koin compiler) over kapt,
which touches more of the build; and keep unit tests on the JVM —
Android-framework test dependencies belong in instrumented tests, not JVM unit
tests.

## Repository order

Gradle asks repositories for a module in declaration order, and the first one that can
serve it wins. Keep the list short and each entry scoped to what it actually hosts, so
resolution doesn't fan out to every repository for every dependency:

- **Order matters** — put the most specific or fastest repositories first.
- **Filter by group** — a `content`/`mavenContent` filter makes a repository answer only
  for the groups it is authoritative for. For example, limit `google()` to the Android
  groups via `includeGroupAndSubgroups(...)`:
  ```kotlin
  google {
      mavenContent {
          includeGroupAndSubgroups("androidx")
          includeGroupAndSubgroups("com.android")
          includeGroupAndSubgroups("com.google")
      }
  }
  ```
- **Scope third-party repos the same way** — a repo like JitPack should serve only the
  groups it is authoritative for, not be an open fallback for anything Maven Central
  doesn't have. JitPack artifacts all live under `com.github.*`:
  ```kotlin
  maven("https://jitpack.io") {
      content {
          includeGroupAndSubgroups("com.github")
      }
  }
  ```
  This stops Gradle querying JitPack for every missing artifact, which is slow and makes
  resolution failures confusing. When a repo serves a group and *nothing else*, an
  `exclusiveContent { forRepository(...) { filter { includeGroup(...) } } }` block is the
  stronger form — but a plain `content` filter is usually enough.

## CI build cache

The build cache must be shared and written only where it's reusable:

- **Route every Gradle job through `gradle-setup`** — the composite action in
  `.github/actions/gradle-setup/action.yml` restores the cache via
  `gradle/actions/setup-gradle`.
- **Keep `cache-encryption-key` set** (`GRADLE_ENCRYPTION_KEY`) — it gates
  configuration-cache data.
- **Branches write branch-local caches** — every job sets `write-cache: true`;
  `setup-gradle` scopes the cache to the branch, so only the default branch's
  cache acts as the shared cache.
- **PR-scoped caches are deleted** by `cache-cleanup.yml` when the PR closes —
  never rely on them persisting.
- **Never pass `--no-daemon`** — it defeats the daemon the cache and parallelism
  depend on.

## Verifying

Confirm the caches actually work after any build-file change:

```bash
# Configuration cache: run twice, second run must reuse the cache
./gradlew help
./gradlew help   # expect: "Reusing configuration cache."

# Fail loudly on configuration-cache problems instead of warning
./gradlew :server:test :shared:jvmTest :app:jvmTest :androidApp:assembleDebug --configuration-cache-problems=fail

# Build cache: a clean build followed by a cache hit proves it works
./gradlew clean :server:test :shared:jvmTest :app:jvmTest :androidApp:assembleDebug
./gradlew :server:test :shared:jvmTest :app:jvmTest :androidApp:assembleDebug --info   # expect FROM-CACHE on the cached tasks
```

- A second run that still reports tasks as `UP-TO-DATE`/`FROM-CACHE` is the
  success signal; a task that re-runs for no reason has undeclared or
  non-relocatable inputs.
- If `--configuration-cache-problems=fail` fails, the configuration cache is
  broken and must be fixed before the change is done.

### Benchmarking

When a change is meant to *improve* build performance (not just keep the caches
working), measure before and after rather than guessing:

```bash
# Profile a build; report lands at build/reports/profile/profile-*.html
./gradlew :androidApp:assembleDebug --profile

# Wall-clock time, warm caches first so the second run reflects steady state
./gradlew :androidApp:assembleDebug
time ./gradlew :androidApp:assembleDebug
```

- Always run the build once first to warm the caches, then time a second run —
  a cold build measures cache population, not steady-state performance.
- Use `--profile` for a per-task breakdown and `--scan` for a shareable build
  scan; both show configuration, task, and dependency-resolution time.
- Compare the same task under the same conditions (same machine, same cache
  state, no `--rerun-tasks` unless that is the point). A single wall-clock run
  is noisy — repeat a few times and report the median.

## Further reading

- [Gradle performance guide](https://docs.gradle.org/current/userguide/performance.html)
  — the canonical reference for build speed, caching, and configuration performance.
