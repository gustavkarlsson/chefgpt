# Finding the latest version

Never guess a version number. Resolve every candidate from an authoritative source.

## Which source hosts what

| Kind | Source | Query |
|---|---|---|
| Maven Central libraries — ktor, koin, coil, kermit, flyway, sqldelight, testcontainers, kotlinx, logback, slf4j, bcrypt, kotlin-result, slapshot | `repo1.maven.org` | `curl -s https://repo1.maven.org/maven2/<group/as/path>/<artifact>/maven-metadata.xml` |
| AGP, `androidx.*`, `com.google.*` | Google Maven | `curl -s https://dl.google.com/dl/android/maven2/<group/as/path>/<artifact>/maven-metadata.xml` |
| Gradle plugin ids — spotless, ktor plugin, sqldelight, slapshot, atomicfu, koin compiler | Plugin Portal marker artifact | `curl -s https://plugins.gradle.org/m2/<id/as/path>/<id>.gradle.plugin/maven-metadata.xml` |
| JitPack-only artifacts | JitPack | `curl -s https://jitpack.io/api/builds/<group>/<artifact>/latest` |
| Gradle itself | `services.gradle.org` | `curl -s https://services.gradle.org/versions/current` (all: `/versions/all`) |
| Android SDK packages | local install | `"$ANDROID_HOME/cmdline-tools/latest/bin/android" sdk list --all` |
| GitHub Actions | GitHub | `gh api repos/<owner>/<repo>/releases/latest --jq .tag_name` |
| Docker images | Docker Hub | `curl -s 'https://hub.docker.com/v2/repositories/<namespace>/<image>/tags?page_size=100' \| jq -r '.results[].name'` — namespace is `library` for official images |

`org.jetbrains.*` multiplatform artifacts — Compose, lifecycle, navigation3 — are published
to Maven Central, not Google Maven, even though they mirror androidx libraries.

`sdkmanager` is deprecated and delegates to the `android` CLI; use `android sdk` directly.
Its listing is lexicographic, so sort before taking the highest:

```bash
"$ANDROID_HOME/cmdline-tools/latest/bin/android" sdk list --all \
  | sed -n '/^Available packages:/,$p' \
  | grep -oE 'platforms/android-[0-9.]+' | sort -uV | tail -5
```

## Reading maven-metadata.xml

The `<latest>` and `<release>` elements are unreliable and can point at a pre-release. Scan
the full `<versions>` list and pick the highest entry yourself, filtering pre-releases
unless the unstable-version policy allows one:

```bash
curl -s <metadata-url> \
  | grep -o '<version>[^<]*' | sed 's/<version>//' \
  | grep -Eiv -- '-(alpha|beta|rc|dev|m[0-9]|preview|snapshot)|\.(beta|rc)[0-9]' \
  | tail -5
```

Drop the `grep -v` when the current version is already unstable and you need to see the
in-range candidates.

Version lists are in publication order, not sorted — `tail` is usually right, but confirm
by eye when a project maintains several release branches (e.g. an active 8.x alongside 9.x).

## Mapping catalog keys to coordinates

A `[versions]` key is not always the artifact name. Read `[libraries]` and `[plugins]` to
find every coordinate that references the key before looking anything up — one key can back
several artifacts that must move together, and a mismatched name hides the real source.
Current example: `slf4jSimple` actually backs `org.slf4j:slf4j-api`.

## Changelogs and migration guides

| Dependency | Where |
|---|---|
| Gradle | https://docs.gradle.org/current/release-notes.html and the upgrade guide for the target major |
| AGP | https://developer.android.com/build/releases/gradle-plugin — includes the AGP↔Gradle compatibility table |
| Android SDK / platform | https://developer.android.com/about/versions |
| Kotlin | https://github.com/JetBrains/kotlin/releases plus "What's new in Kotlin X" on kotlinlang.org |
| Compose Multiplatform | https://github.com/JetBrains/compose-multiplatform/releases and the Kotlin compatibility table at https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compatibility-and-versioning.html |
| Ktor | https://ktor.io/docs/whats-new.html and the migration guides under ktor.io/docs |
| Koin | https://github.com/InsertKoinIO/koin/releases |
| SQLDelight | https://github.com/sqldelight/sqldelight/releases |
| Flyway | https://documentation.red-gate.com/flyway/release-notes-and-older-versions |
| Testcontainers | https://github.com/testcontainers/testcontainers-java/releases |
| Coil | https://github.com/coil-kt/coil/releases and coil-kt.github.io upgrade guides |
| Spotless / ktlint | https://github.com/diffplug/spotless/blob/main/CHANGES.md, https://github.com/pinterest/ktlint/releases |
| GitHub Actions | the action's own releases page |

For anything not listed, prefer the project's GitHub releases page, then its docs site.
Use WebFetch/WebSearch — do not rely on recollection for what a release changed.
