# ChefGPT

A Kotlin Multiplatform toy project exploring custom AI agents and KMP.

## Prerequisites

Only Java is always required. Everything else depends on which platforms you work on —
building just the server, or just the desktop app, does not need the Android or iOS tooling.

### Java 25 (always)

Any JDK 25 works, from any source — Gradle's toolchain resolver will even download one if
your machine has none.

If you happen to use [SDKMAN](https://sdkman.io/), `sdk install` picks up the version
recommended in `.sdkmanrc`, and `sdkman_auto_env=true` applies it automatically.

### Desktop

Nothing beyond Java.

### Web

Nothing beyond Java — Gradle downloads Node and yarn itself on the first build.

### Server

Docker, to run the databases in containers. Install [Docker](https://docs.docker.com/get-docker/) and ensure the Docker daemon is running.

### Android

- Android SDK with API level 37
- A connected device or running emulator

### iOS

- Xcode with iOS 18.2 SDK
- A simulator or a connected device

## Setup

Create the local server dev config (only needed once):

```bash
./setup_dev.sh
```

Tweak the created file to your liking.

### Checking your environment

If you use Claude Code, the `setup` skill checks that your machine has what the platforms
you work on need, and offers to install anything missing:

```
/setup
```

It asks which platforms you care about, and can optionally build, install and run each one
to confirm the toolchain end to end. To skip the questions:

```
/setup --platforms all --verify
```

### Spotless pre-commit hook (optional)

Automatically formats Kotlin files before each commit:

```bash
./install-spotless-pre-commit-hook.sh
```

## Building & Testing

Run JVM tests and lint:

```bash
./gradlew spotlessCheck :server:test :shared:jvmTest :shared:testAndroidHostTest :app:jvmTest :app:testAndroidHostTest
```

Run all tests including Android and iOS (requires platform tools):

```bash
./gradlew check
```

## Running

### Server

Start the containerized database and the Ktor server:

```bash
./run_server.sh
```

To manage the database yourself, run the server alone:

```bash
./gradlew :server:run
```

### Desktop

Build and run the desktop JVM app with hot reload:

```bash
./run_desktop.sh
```

### Android

Build the debug APK, then install and launch it on the connected device:

```bash
./run_android.sh
```

Debug builds are signed with the checked-in `androidApp/debug.keystore`, using
the standard Android debug credentials (store and key password `android`, alias
`androiddebugkey`), so every machine and CI produce an identically signed APK.
Its certificate fingerprint is:

```
SHA-1: 88:8A:0F:A1:47:A8:7B:0C:6C:29:7E:65:9B:DD:F4:35:BA:EF:8A:BA
```

A debug build installed before this keystore existed was signed with the
machine-local `~/.android/debug.keystore` and must be uninstalled once before
the new one will install.

### iOS

Build the debug app, then install and launch it on a connected iPhone, falling
back to a simulator (booting or creating one) when no device is attached:

```bash
./run_ios.sh
```

Set `SIMULATOR_DEVICE` to pick another device type, as listed by
`xcrun simctl list devicetypes`. Running on a physical device needs `TEAM_ID`
set in `iosApp/Configuration/Config.xcconfig`.

Alternatively open `iosApp/iosApp.xcodeproj` in Xcode and run the scheme, or
build the framework manually:

```bash
./gradlew :app:linkDebugFrameworkIosSimulatorArm64
```

### Web

Start a dev server for the JS or Wasm target:

```bash
./gradlew :app:jsBrowserDevelopmentRun
./gradlew :app:wasmJsBrowserDevelopmentRun
```

Both default to port 8080, so stop the Ktor server first.

### Server + Desktop app together

Run `./run_server.sh` and `./run_desktop.sh` in separate terminals.
