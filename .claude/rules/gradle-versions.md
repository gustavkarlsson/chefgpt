---
description: Where dependency versions live and how they must be declared.
paths:
  - "**/*.gradle.kts"
  - "gradle/libs.versions.toml"
---

# Dependency versions

Every library, plugin and tool version belongs in `gradle/libs.versions.toml`, under `[versions]`,
and is referenced with `version.ref`. Never an inline version literal — not in the catalog's
`[libraries]`/`[plugins]` entries, and not in a `build.gradle.kts`.

A few versions live outside the catalog because their format can't reference it:

| File | What lives there |
|---|---|
| `gradle/wrapper/gradle-wrapper.properties` | the Gradle distribution (`-bin` variant) |
| `settings.gradle.kts` | the inline `foojay-resolver` version |
| `.github/` | action pins across every workflow and composite action |

A version anywhere else is in the wrong place — move it to the catalog.

To upgrade anything, use the **update-dependencies** skill: several versions are coupled (AGP,
Gradle, `compileSdk`, the installed Android SDK packages) and bumping them in the wrong order
breaks the build.
