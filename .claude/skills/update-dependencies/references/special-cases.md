# Per-dependency playbooks

Consult the entry for anything you are about to bump. Dependencies not listed here have no
special handling beyond the changelog and migration policy in `SKILL.md`.

## Gradle wrapper

Always use the **`-bin`** distribution — it is a much smaller download, which keeps CI and
fresh checkouts fast.

```bash
./gradlew wrapper --gradle-version <version> --distribution-type bin
./gradlew wrapper --gradle-version <version> --distribution-type bin   # second run updates the wrapper jar and scripts
./gradlew tasks                                                        # actually downloads and installs the distribution
```

The second `wrapper` run is required: the first one regenerates `gradle-wrapper.properties`
using the *old* wrapper, the second regenerates the jar and scripts using the new one.
`./gradlew tasks` is what makes the new distribution real on disk — without it you have
only edited a properties file.

Afterwards check `distributionUrl` still ends in `-bin.zip`, and that the configuration
cache (`org.gradle.configuration-cache=true` in `gradle.properties`) still works — Gradle
majors tighten configuration-cache rules, and the `postgres` and `buildSupported` tasks use
`ProcessBuilder` / `providers.exec`.

Bump the wrapper **before** AGP — unless their supported ranges do not overlap, in which
case both move in one commit. See the AGP section.

## Android SDK and compileSdk

Use the `android` CLI — `sdkmanager` is deprecated and only forwards to it.

```bash
ANDROID="$ANDROID_HOME/cmdline-tools/latest/bin/android"
"$ANDROID" sdk list --all      # available packages; see version-lookup.md for sorting
"$ANDROID" sdk install <package>
"$ANDROID" sdk update          # updates already-installed packages
```

Name packages exactly as `sdk list` prints them (`platforms/android-<N>`). Install and
update download in the foreground and may prompt for a licence, so run them with a generous
timeout and never in the background.

Then set `androidCompileSdk` in `libs.versions.toml` to `<N>`. It is consumed by
`app/build.gradle.kts`, `androidApp/build.gradle.kts` and `shared/build.gradle.kts` through
the catalog, so one edit covers all three.

Only install stable platforms — preview platforms are named after the release letter rather
than a number, and are subject to the unstable-version rule.

**Never change `androidTargetSdk`.** Raising `targetSdk` opts the app into new runtime
behaviour changes and is a product decision, not a dependency update.

A newer `compileSdk` can surface new lint checks and deprecation warnings — run
`./gradlew :androidApp:lintDebug` and read them.

## Android Gradle Plugin

Check the AGP↔Gradle compatibility table at
https://developer.android.com/build/releases/gradle-plugin before bumping, and upgrade the
wrapper first if the target AGP needs a newer Gradle. AGP also declares a minimum JDK — it
must be satisfied by the pinned `jvmToolchain`; if it is not, stop and ask, because the
toolchain is pinned by the Hot Reload constraint.

AGP majors routinely remove DSL and change variant APIs. Read the release notes in full.

Gradle and AGP constrain each other in **both** directions, so the wrapper cannot always go
first: Gradle 9.6 removed an internal API AGP 8.x depends on, while AGP 9.4 requires Gradle
9.6+. When the supported ranges do not overlap, bump both in one commit.

`app` and `shared` use the KMP-specific `com.android.kotlin.multiplatform.library` plugin,
configured through an `android { }` block *inside* `kotlin { }`. Only `androidApp` uses
`com.android.application`, with a top-level `android { }`. Since AGP 9 the plain
`com.android.library` / `com.android.application` plugins are rejected in any module that
also applies the Kotlin Multiplatform plugin — do not reintroduce them.

## Kotlin

One `version.ref` moves `kotlinJvm`, `kotlinMultiplatform`, `kotlinSerialization` and
`composeCompiler` together. After the bump:

1. **Refresh the committed JS/Wasm lockfiles** in `kotlin-js-store/`:
   ```bash
   ./gradlew kotlinUpgradeYarnLock
   ./gradlew tasks --all | grep -i yarnlock   # find the wasm-specific task name
   ```
   Both `kotlin-js-store/yarn.lock` and `kotlin-js-store/wasm/yarn.lock` are committed and
   will otherwise fail the JS/Wasm builds.
2. **Re-check the compiler options** in the root `build.gradle.kts`: the `optIn` list and
   the `-Xexpect-actual-classes` free arg. Opt-ins get promoted to stable across releases,
   at which point the opt-in becomes a warning and should be dropped.
3. **Migrate to new syntax.** Read "What's new in Kotlin X" and, for every
   *non-experimental* language feature that supersedes something the codebase does, migrate
   the usages. This is required, not optional.
4. **Confirm Compose Multiplatform supports the Kotlin version** before committing — see
   below.
5. Verify with `./gradlew buildSupported`, not just the verify skill; JS and Wasm are where
   Kotlin bumps usually break first.

## Compose Multiplatform

`composeMultiplatform`, `composeHotReload` and `material3` all track the Kotlin version.
Check the compatibility table at
https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compatibility-and-versioning.html
before moving either Kotlin or Compose.

`material3` is on an alpha line (`org.jetbrains.compose.material3`) released independently
of the Compose Multiplatform BOM — apply the unstable-version rule, and prefer a stable
release if one has appeared.

`composeHotReload` is the reason `jvmToolchain` is pinned: the toolchain must stay within
what the JetBrains Runtime that Hot Reload provisions supports. A Hot Reload bump can
*raise* that ceiling, but never move the toolchain without launching `:app:hotRunJvmAsync`
to confirm the app actually starts.

## Ktor

`io.ktor.plugin` and every `ktor-*` artifact share one `version.ref`; they must stay
identical. The Gradle plugin version being ahead of or behind the runtime is a common
source of confusing failures.

Ktor minors regularly deprecate plugin installation and routing DSL. After a bump, check
`server/src/main/kotlin/se/gustavkarlsson/chefgpt/plugins/` and `routes/`, plus the client
setup in `shared/`, for deprecation warnings.

`koog-ktor` depends on Ktor — check its constraint before moving Ktor.

## Koin

`koin-annotations` currently shares `version.ref = "koin"`. Before bumping, confirm the
`io.insert-koin:koin-annotations` coordinate actually publishes that version — it has
historically had its own version line, and a shared ref may be wrong. If it does have an
independent line, split it into its own `[versions]` key.

`koinPlugin` (`io.insert-koin.compiler.plugin`) versions separately from the runtime and is
KSP/Kotlin-sensitive — bump it alongside Kotlin and re-run the build to confirm generated
DI code still compiles.

## koog

`koogAgents` and `koogKtor` are a deliberately pinned matched pair: `koog-ktor` trails
`koog-agents` by a `-beta` suffix and pulls `koog-agents` in transitively. Move both
together to the same version, or move neither. Do not resolve `koog-agents` to a version
`koog-ktor` does not expect.

## SQLDelight

A bump regenerates the database interface and re-runs migration verification
(`verifyMigrations = true`, `deriveSchemaFromMigrations = true`). The server build also
copies SQLDelight migrations into the Flyway resource directory via
`copySqldelightMigrationsToFlywheel`, so a change in migration file naming or output layout
breaks Flyway too.

Run `./gradlew :server:test` after, and check `server/src/main/sqldelight/databases` for an
unexpected schema diff before committing.

## Flyway

`flyway-core` and `flyway-database-postgresql` share a `version.ref` and must stay equal.
Flyway majors change migration checksum and validation behaviour — read the release notes
carefully, and confirm the Postgres version in the `postgres` task is still supported.

## Testcontainers

`testcontainers-junit-jupiter` and `testcontainers-postgresql` share a `version.ref`. Bumps
can change container lifecycle and reuse defaults; verify with `./gradlew :server:test`
against a running Docker daemon.

## ktlint (via Spotless)

`ktlint` is consumed by Spotless (`ktlint(libs.versions.ktlint.get())` in the root
`build.gradle.kts`), not as a dependency. A bump changes formatting rules and will reformat
files across the repo.

Sequence it as: bump the version → `./gradlew spotlessApply` → **commit the reformat
separately** from the version bump, so the version change stays reviewable.

`spotless` (the plugin) and `ktlint` (the engine) version independently; Spotless pins a
supported ktlint range, so bump the plugin first if ktlint is rejected.

## Postgres — driver and image

`postgresDriver` (`org.postgresql:postgresql`) is the JDBC driver; `postgresImage` is the
Docker image tag used by the `postgres` task in `server/build.gradle.kts`. They release on
independent schedules, but the driver must stay compatible with the image's major version —
check the driver's changelog when the image crosses a major.

`postgresImage` is resolved from Docker Hub, not Maven; see `version-lookup.md`.

## androidx and JetBrains multiplatform androidx

`androidxActivity` comes from Google Maven; `androidxLifecycle` and `navigation3` are the
JetBrains multiplatform ports on Maven Central under `org.jetbrains.androidx.*` and version
independently from the Google originals. Do not copy a version from one to the other.

`navigation3` is on a beta line — apply the unstable-version rule. It is also coupled to
`koin-compose-navigation3` and `lifecycle-viewmodel-navigation3`; bump those in step.

## GitHub Actions

Pin to the major tag (`actions/checkout@v4` → `@v5`), matching the existing convention.
Read the release notes for runtime bumps (Node version) and behavioural changes — the
artifact actions in particular have changed semantics across majors.

Currently in use: `actions/checkout`, `actions/upload-artifact`, `actions/setup-java`,
`gradle/actions/setup-gradle`.

Do not change `java-version` in `.github/actions/gradle-setup/action.yml` — it must track
`jvmToolchain`, which is pinned.

## foojay-resolver

Applied in `settings.gradle.kts`, where there is no `libs` accessor, so its version stays an
inline literal — bump it in place. Released to the Gradle Plugin Portal as
`org.gradle.toolchains.foojay-resolver-convention`.

## Java toolchain — flag only

`jvmToolchain` in `libs.versions.toml`, `.sdkmanrc`, and `java-version` in
`.github/actions/gradle-setup/action.yml` are pinned to a JDK that works with the JetBrains
Compose Hot Reload JVM, and move as one. Out of scope for a dependency update — if an
upgrade demands a newer JDK, stop and report it rather than bumping.
