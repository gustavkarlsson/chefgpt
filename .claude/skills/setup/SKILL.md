---
name: setup
description: Check that the local development environment is correctly set up for the platforms you work on — JDK, Docker, the server dev config, the Android SDK, Xcode — fixing the safe gaps and offering to install the rest. Use when builds fail for environment reasons, when onboarding a machine, or when asked to verify the toolchain. Takes `--platforms server,android,ios,desktop,web` (or `all`) and `--verify`; asks when either is omitted.
---

This skill checks the **machine**. The **verify** skill checks the **code** — if the
question is "do the tests pass", use that one instead.

Every platform is optional. Only the JDK and the Gradle wrapper are unconditional; check
nothing else unless a selected platform needs it.

---

## 1. Decide the scope

Two inputs. Read them from the arguments, and **ask for whichever is missing** — never
assume.

| Argument | Values | If absent |
|---|---|---|
| `--platforms` | comma-separated `server`, `android`, `ios`, `desktop`, `web` — or `all` for every one of them | ask, multi-select |
| `--verify` | present / absent | ask, yes or no |

When either is missing, use the **AskUserQuestion** tool. If both are missing, ask both in
a single call rather than two round trips:

- *Which platforms do you develop on this machine?* — `multiSelect: true`, one option per
  platform, each described by what it costs to set up (`server` → Docker + Postgres;
  `android` → Android SDK + an emulator or device; `ios` → Xcode + a simulator or device;
  `desktop` and `web` → nothing beyond the JDK).
- *Also build, install and run each one?* — `Check only` (fast, seconds) vs
  `Check and verify` (builds and launches each platform; minutes per platform, and much
  longer on a cold build cache).

`--platforms all` is shorthand for every platform — expand it to the full list and skip the
question. On macOS that is all five; elsewhere it is everything except `ios`.

Pre-select nothing on the platform question. A user who picks only `server` has a valid
setup; never treat the unselected platforms as missing, and never check them.

What each selected platform needs:

| Platform | Requires |
|---|---|
| *(always)* | a JDK Gradle can run on, and the Gradle wrapper |
| `server` | Docker daemon, the pinned Postgres image, `server/application_dev.conf` |
| `desktop` | nothing beyond the JDK |
| `web` | nothing beyond the JDK — Gradle provisions Node and yarn itself, so the first build needs network |
| `android` | Android SDK: platform, build-tools, platform-tools, and an emulator or a connected device |
| `ios` | macOS, Xcode, and a simulator or a connected device |

---

## 2. Read the required versions from the repo

Never hardcode these — read them, so the skill does not rot when they are bumped.

```bash
JDK=$(sed -n 's/^java=\([^ #]*\).*/\1/p' .sdkmanrc)                                        # 21.0.10-zulu
COMPILE_SDK=$(sed -n 's/^androidCompileSdk *= *"\(.*\)"/\1/p' gradle/libs.versions.toml)   # 36
PG_IMAGE=$(sed -n 's/^postgresImage *= *"\(.*\)"/\1/p' gradle/libs.versions.toml)          # 18.3
GRADLE=$(sed -n 's|.*/gradle-\([0-9.]*\)-all.zip|\1|p' gradle/wrapper/gradle-wrapper.properties)
IOS_TARGET=$(sed -n 's/.*IPHONEOS_DEPLOYMENT_TARGET = \([^;]*\);.*/\1/p' \
    iosApp/iosApp.xcodeproj/project.pbxproj | head -1)                                     # 18.2
```

---

## 3. Check the JVM and Gradle — always

```bash
./gradlew --version    # Gradle version + the JVM Gradle actually picked
java -version 2>&1     # the JDK on PATH
```

`.sdkmanrc` records the recommended JDK, but **SDKMAN is not required** — it is one way to
manage JDKs, not the way this project builds. Don't assume it is installed, and don't tell
the user to adopt it. What actually matters is that Gradle can get a JDK 21 toolchain:
`jvmToolchain(21)` plus the foojay resolver in `settings.gradle.kts` means Gradle will
download one itself if the machine has none. So:

- **`./gradlew --version` works** → this area passes. `jvmToolchain(21)` governs every
  compile task and the toolchain-aware run tasks, regardless of the daemon's own JVM.
- **The JVM Gradle runs on is not 21** → a *warning*, worth a line in the report but not a
  blocker, as long as the run tasks used in step 9 are the toolchain-aware ones.

Confirm a JDK 21 exists somewhere Gradle can see — including one Gradle provisioned itself
under `~/.gradle/jdks`. Ask Gradle rather than presuming a manager or trusting
`/usr/libexec/java_home`, which happily returns a 17 when asked for 21:

```bash
./gradlew -q javaToolchains | grep -B1 -A3 'Language Version: *21'   # every JDK 21 Gradle knows about
```

If none exists, offer to install one (step 8) — though the foojay resolver will provision
one on the next build anyway.

---

## 4. Check `server`

```bash
docker info >/dev/null 2>&1                                 # daemon reachable, not just the client binary
docker image inspect "postgres:$PG_IMAGE" >/dev/null 2>&1   # pinned image present locally
docker inspect -f '{{.State.Running}}' chefgpt-postgres     # the container the :server:postgres task manages
test -f server/application_dev.conf                         # local overrides, created by ./setup_dev.sh
lsof -nP -iTCP:8080 -iTCP:5432 -sTCP:LISTEN                 # something already holding the server / db ports?
```

`server/application_dev.conf` is gitignored and holds real API keys — **never print its
contents**, only whether it exists.

A missing image is fine; `:server:postgres` pulls on first use. Report it, do not pre-pull.

---

## 5. Check `desktop` and `web`

Neither needs anything beyond step 3. The Kotlin/JS and Wasm toolchains provision their own
Node and yarn into `~/.gradle` on first use, resolving against the committed lockfiles in
`kotlin-js-store/` — so `web` only needs network the first time. Note both in the report as
covered by the JDK, so the user can see they were considered rather than skipped.

---

## 6. Check `android`

```bash
ANDROID="$ANDROID_HOME/cmdline-tools/latest/bin/android"   # never `sdkmanager` — deprecated, forwards here
ADB="$ANDROID_HOME/platform-tools/adb"
test -n "$ANDROID_HOME" && test -x "$ANDROID" && test -x "$ADB"
test -d "$ANDROID_HOME/platforms/android-$COMPILE_SDK"
ls "$ANDROID_HOME/build-tools" | grep "^$COMPILE_SDK\."
"$ADB" devices                                    # connected devices and running emulators
"$ANDROID_HOME/emulator/emulator" -list-avds      # AVDs available to boot, if the emulator is installed
```

Resolve `adb` from `$ANDROID_HOME/platform-tools`, **not** from PATH — a Homebrew `adb`
can shadow it while the SDK itself is incomplete. (This is why the `buildSupported` task's
`which adb` heuristic in `build.gradle.kts` is not trustworthy as a gate.)

Android needs the SDK pieces above **and somewhere to run the app — either a connected
device or an emulator**. A connected device is enough on its own: if `adb devices` lists
one, the emulator and AVDs are irrelevant, so do not report them as missing or offer to
install them. Only when no device is attached does the emulator become necessary.

A partial SDK install is not ready and gets the same offer as a missing one.

---

## 7. Check `ios`

On a non-macOS host, report iOS as unavailable on this OS and move on — do not offer to
install anything.

```bash
xcode-select -p                                      # must resolve into Xcode.app, not CommandLineTools
xcodebuild -version
xcrun --sdk iphonesimulator --show-sdk-version       # must be >= $IOS_TARGET
xcrun simctl list devices available                  # simulators on a runtime >= $IOS_TARGET
xcrun devicectl list devices 2>/dev/null             # physically connected iPhones/iPads
```

iOS also needs **a simulator or a connected device**. A connected device counts, so do not
offer to download simulator runtimes when one is attached.

`iosApp/Configuration/Config.xcconfig` leaves `TEAM_ID` empty. Simulator builds work
without it; a physical device additionally needs signing, so if the user intends to build
to a device, flag the empty `TEAM_ID` as the blocker. Report it, **never edit it** — it is
the user's Apple team.

---

## 8. Fix what is broken

Only for selected platforms. Never hand the user a command to copy-paste — every gap is
either fixed outright or offered as an action to perform.

**Apply immediately, without asking** — fast, reversible, no downloads:

```bash
./setup_dev.sh                    # when server/application_dev.conf is missing
open -a Docker                    # when the daemon is down; then poll `docker info` for up to ~60s
```

**Offer, then run on confirmation.** Ask **once**, with one option per platform that needs
work — `Set up Android`, `Set up iOS`, `Install helper CLIs`, `Skip` — not one prompt per
package. List exactly what each option installs. Declining is a first-class answer: record
it and respect it for the rest of the run, including step 10.

| Gap | What to offer to run |
|---|---|
| No usable JDK | install `$JDK` with whichever manager step 3 found — e.g. `sdk install java $JDK` if SDKMAN is present, otherwise `brew install --cask temurin@21` or the platform equivalent. If none is available, say so: Gradle's toolchain resolver will provision one on the next build anyway. |
| Missing platform, build-tools or platform-tools | `"$ANDROID" sdk install <package>` |
| No Android device connected **and** no emulator installed | `"$ANDROID" sdk install` the emulator and a system image |
| No Android device connected **and** no AVD | `avdmanager create avd` against a system image for `$COMPILE_SDK` |
| No iOS device connected **and** no simulator runtime >= `$IOS_TARGET` | `xcodebuild -downloadPlatform iOS` |
| No iOS device connected **and** no simulator on a valid runtime | `xcrun simctl create <name> <devicetype> <runtime>` |
| Missing `rg` / `jq` / `gh` | `brew install <formula>` |

Android SDK installs may prompt to accept a licence — run them in the **foreground** with a
generous timeout, never backgrounded. Re-check the area afterwards and report the result.

Also check the CLIs other skills rely on, regardless of platform selection: `rg` (used by
**android-min-sdk**), `curl`, `jq`, `gh` (used by **update-dependencies** version lookups),
and `git`. Missing ones only degrade other skills, never a build.

---

## 9. Verify — only when verification was requested

Say up front how long it will take — minutes per platform, and considerably longer on a
cold build cache. The stages that run are exactly the selected platforms that are ready,
after the step 8 answers. A platform the user declined to fix is
not built and not run.

Run every Gradle invocation as-is. Don't set `JAVA_HOME` and don't invoke a JDK manager —
the toolchain resolves JDK 21 on its own, provided you use the task names below.

**1. Build.** Drive explicit tasks rather than `./gradlew buildSupported` — that task picks
targets from `which adb` / `which xcodebuild` at configuration time and downgrades misses to
a `logger.warn` while still exiting 0, so it cannot tell you what actually built. One task
per selected platform:

| Platform | Task |
|---|---|
| `server` | `:server:shadowJar` |
| `desktop` | `:app:jvmJar` |
| `web` | `:app:jsBrowserDevelopmentExecutableDistribution` and `:app:wasmJsBrowserDevelopmentExecutableDistribution` |
| `android` | `:app:assembleDebug` |
| `ios` | `:app:linkDebugFrameworkIosSimulatorArm64` |

**2. Server.** Start it, prove it answers, stop it. There is no health endpoint — any HTTP
status proves the server is up; only a refused connection is a failure.

```bash
./gradlew :server:postgres          # starts/creates the chefgpt-postgres container
./gradlew :server:run &             # background
curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/   # poll until non-empty
```

**3. Desktop.** `./gradlew :app:run &` — it opens a real window. Confirm the process
survives startup (`pgrep -f se.gustavkarlsson.chefgpt.MainKt`), then kill it.

**4. Web.** Confirm the distributions built above produced an `index.html`. Locate it
rather than assuming a path — the output directory moves between Kotlin versions:

```bash
find app/build/dist -name index.html    # e.g. app/build/dist/js/productionExecutable/index.html
```

Then serve one and prove it answers:

```bash
./gradlew :app:jsBrowserDevelopmentRun &                        # webpack dev server
curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/   # expect 200, then kill
```

`browser()` is left unconfigured in `app/build.gradle.kts`, so the dev server takes
webpack's default port — it binds `*:8080`, **the same port the Ktor server uses**. Never
run this while the server stage is still up: stop the server first, or skip the dev-server
run and treat the built `index.html` as sufficient evidence.

Two web-specific traps. Build the **development** distributions, not
`:app:jsBrowserDistribution` / `:app:wasmJsBrowserDistribution` — the production webpack
pass takes ~14 minutes and proves nothing extra about the environment. And the first web
build on a cold machine can fail once in `:kotlinStoreYarnLock` on a transitive npm version
clash; retry once before reporting a failure, and never leave a modified
`kotlin-js-store/yarn.lock` behind.

**5. Android.** Prefer whatever is already attached — if `adb devices` lists a device or a
running emulator, use it. Only when nothing is attached, boot an existing AVD; never create
one here. Installing on the user's own physical device is fine, but say which target you
used.

Booting an emulator takes minutes, so start it *before* the build stage to overlap the
wait — but re-check it here, and wait for the boot to actually finish. `adb devices` shows
`offline` long before the device is usable:

```bash
"$ANDROID_HOME/emulator/emulator" -avd <name> -no-snapshot-save &
"$ADB" wait-for-device
until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 5; done
```

```bash
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" shell am start -n se.gustavkarlsson.chefgpt/.MainActivity
"$ADB" shell pidof se.gustavkarlsson.chefgpt     # must be non-empty
"$ADB" logcat -d -s AndroidRuntime:E             # must be free of crashes
"$ADB" shell am force-stop se.gustavkarlsson.chefgpt && "$ADB" uninstall se.gustavkarlsson.chefgpt
```

**6. iOS.** Mirrors the (currently disabled) `build-ios` job in
`.github/workflows/verify.yml`. Use the simulator even when a physical device is connected —
installing to a device needs a signing team, and `TEAM_ID` is empty by default.

Boot the simulator **at this stage**, not earlier: a simulator booted before the build can
shut itself down during a long one, and `simctl install` then fails with
`No devices are booted.` Boot it and wait, rather than assuming it is still up:

```bash
xcrun simctl boot <udid> 2>/dev/null || true     # already-booted is not an error
xcrun simctl bootstatus <udid> -b                # blocks until it is actually usable
```

```bash
xcodebuild build -project iosApp/iosApp.xcodeproj -configuration Debug -scheme iosApp \
    -sdk iphonesimulator -derivedDataPath build/setup-ios
APP=build/setup-ios/Build/Products/Debug-iphonesimulator/chefgpt.app
xcrun simctl install booted "$APP"
xcrun simctl launch booted se.gustavkarlsson.chefgpt.chefgpt   # bundle id resolves this way while TEAM_ID is empty
xcrun simctl terminate booted se.gustavkarlsson.chefgpt.chefgpt
xcrun simctl uninstall booted se.gustavkarlsson.chefgpt.chefgpt
```

**7. Clean up.** Kill every backgrounded process, uninstall both test apps, and state what
was deliberately left running (the `chefgpt-postgres` container, and any emulator or
simulator that was already up).

---

Report: which platforms were selected and how, then a table of the checked areas with a
verdict — ✅ ok, ⚠️ warning, ❌ not ready, or ⏭️ unavailable on this OS. Then what was
auto-fixed, what was installed after confirmation, and what the user declined. When
verification ran, add the pass/fail of each build/install/run stage.

Never claim a stage passed when it was skipped, and never mention a platform the user did
not select — say "ready for server and desktop" rather than listing Android and iOS as
problems. The one thing to hand back as a manual step is the one this skill genuinely
cannot do: filling in `TEAM_ID` for physical-device builds.
