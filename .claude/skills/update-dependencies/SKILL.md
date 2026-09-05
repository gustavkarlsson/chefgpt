---
name: update-dependencies
description: Upgrade project dependencies — version catalog entries, Gradle wrapper, Android Gradle Plugin, Android SDK / compileSdk, CI actions and Docker images. Use when asked to update, upgrade, bump, or check for newer versions of any dependency, plugin, or tool in this repo.
---

# Updating dependencies

Versions are spread beyond the version catalog, and several are coupled such that bumping
them in the wrong order breaks the build. Follow the ordering and the gates below.

If the user named specific dependencies, scope the work to those (plus anything they
force, e.g. a Gradle bump required by a new AGP). Otherwise cover everything in scope.

## Scope

| File | What lives there |
|---|---|
| `gradle/libs.versions.toml` | all library, plugin and tool versions — the primary target |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle distribution (`-bin` variant) |
| `settings.gradle.kts` | inline `foojay-resolver` version |
| `.github/` | action pins across every workflow and composite action |
| `*/build.gradle.kts` | any version not yet in the catalog |
| local Android SDK | installed packages backing `androidCompileSdk` |

## Workflow

1. **Inventory.** Confirm a clean tree with `git status`. Read `libs.versions.toml`, the
   wrapper properties, `settings.gradle.kts`, everything under `.github/`, and each
   module's `build.gradle.kts` — grep the build scripts for version-shaped literals to
   catch anything that has drifted out of the catalog.
2. **Look up latest versions** for every entry — see `references/version-lookup.md`. Batch
   the lookups; they are independent.
3. **Classify** each candidate against the version policy below into `apply`, `ask`, or
   `skip`.
4. **Read changelogs** for everything that is not a pure patch bump.
5. **Apply in the order below**, consulting `references/special-cases.md` for any entry
   it covers.
6. **Migrate** — new syntax and deprecated APIs, per the migration policy.
7. **Verify and commit** in groups.
8. **Report** — a table of `dependency | old | new | notes`, plus everything deferred or
   skipped and why.

## Ordering

Respect this order; each step constrains the next.

1. **Gradle wrapper** — AGP requires a minimum Gradle version.
2. **Android SDK** via the `android` CLI, then `androidCompileSdk` to match.
3. **AGP** — check compatibility against the Gradle version from step 1.
4. **Kotlin** — moves `kotlinJvm`, `kotlinMultiplatform`, `kotlinSerialization` and
   `composeCompiler` atomically (they all use `version.ref = "kotlin"`).
5. **Compose Multiplatform**, `composeHotReload`, `material3` — gated on the Kotlin version.
6. **Everything else** in the catalog.
7. **CI actions**, **Docker images**, **stray inline versions**.

## Version policy

**Changelogs.** For anything beyond a patch bump (semver), find the official changelog and
any migration guide and analyse them before applying. Patch bumps may go in without one,
but still list them in the report.

**New majors.** Never take a major bump on your own initiative. Collect every available
major, then ask the user which to take — presenting for each: what the release brings, and
**emphasising the incompatible changes that would need migrating**. Ask once, for all of
them together.

**Unstable versions.** Never move *into* alpha, beta, rc, preview, dev, milestone or
snapshot. Three exceptions:

- the user explicitly asked for it;
- staying within the same unstable range (`alpha08 → alpha09`, `preview7 → preview8`);
- moving toward stability (`alpha → beta → rc → stable`).

Where the catalog already pins an unstable version, the last two exceptions apply: do offer
the in-range and toward-stable bumps, and prefer a stable release once one exists.

## Migration policy

Apply **trivial** migrations automatically — renamed symbols, moved packages, mechanical
API replacements, changed defaults, and new non-experimental Kotlin syntax that supersedes
what the codebase currently uses.

**Ask** about non-trivial ones — behavioural changes, anything needing a design decision,
or anything touching many call sites in a non-mechanical way.

Do not stop at "it compiles." Scan the changelog for newly **deprecated** APIs and migrate
those too, and read the build output for new deprecation warnings after each risky upgrade.

Batch every question — majors and non-trivial migrations alike — into a single round.

## Catalog hygiene

- Catalog versions must use `version.ref`, never an inline literal (see `AGENTS.md`).
- When you find a version outside the catalog, move it in — as long as the consumer can
  reach `libs.versions.*` and the result is not more awkward than the literal was. Read it
  at configuration time into a local `val`, never inside a task action, so the
  configuration cache stays valid.

## Hard constraints

Do not change these unless the user explicitly instructs you to:

- **`androidMinSdk`** — raising it drops devices, which is a product decision.
- **`androidTargetSdk`** — never bump it as part of a dependency update, even when
  `androidCompileSdk` moves. Raising it opts the app into new OS behaviour, which is a
  product decision.
- **iOS `IPHONEOS_DEPLOYMENT_TARGET`, `SDKROOT` and `SWIFT_VERSION`** in
  `iosApp/iosApp.xcodeproj/project.pbxproj` — the same reasoning as `androidMinSdk` and
  `androidTargetSdk`. iOS has no separate target setting: the SDK you build against *is*
  the behaviour opt-in. The iOS CI job is also currently disabled (`if: false`), so such a
  change would go unverified.
- **`jvmToolchain` must stay at a version that works with the JetBrains Compose Hot Reload
  JVM** — that is why it is pinned at 21 (see the comment in `libs.versions.toml`). If a
  bump is ever requested, `libs.versions.toml`, `.sdkmanrc` and
  `.github/actions/gradle-setup/action.yml` must move together, and Hot Reload support for
  the new JDK must be confirmed first.
- **Matched pairs** — some catalog entries are deliberately pinned to each other with an
  explanatory comment (currently `koogAgents` / `koogKtor`). Read the comments in
  `libs.versions.toml` and move such entries together or not at all.

## Verify and commit

- Put **all trivial and patch bumps in one commit**, then run the **verify** skill once.
- Give **each risky upgrade its own commit and verification pass**: Gradle, Android SDK /
  `compileSdk`, AGP, Kotlin, Compose Multiplatform, and any accepted major.

Verification depth:

| Upgrade | Also run |
|---|---|
| any | the **verify** skill (`spotlessCheck` + JVM tests) |
| Gradle, AGP, `compileSdk` | `./gradlew :app:lintDebug :app:assembleDebug` |
| Kotlin, Compose | `./gradlew buildSupported` — covers JS and Wasm, which the verify skill does not |
| ktlint | `./gradlew spotlessApply`, committed **separately** from the version bump |
| SQLDelight, Flyway | `./gradlew :server:test` (migration verification is enabled) |
