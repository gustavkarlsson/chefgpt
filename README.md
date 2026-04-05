# ChefGPT

A Kotlin Multiplatform toy project exploring custom AI agents and KMP.

## Prerequisites

### Java 21

If you have [SDKMAN](https://sdkman.io/), you can run `sdk install` to set the recommended JDK (installing if needed)

This will happen automatically if you have `sdkman_auto_env=true` set in your SDKMAN config.

### Android

- Android SDK with API level 36
- A connected device or running emulator

### iOS

- Xcode with iOS 18.2 SDK

### Docker

Required to run the server databases in containers. Install [Docker](https://docs.docker.com/get-docker/) and ensure the Docker daemon is running.

## Setup

Create the local server dev config (only needed once):

```bash
./setup_dev.sh
```

Tweak the created file to your liking.

### Spotless pre-commit hook (optional)

Automatically formats Kotlin files before each commit:

```bash
./install-spotless-pre-commit-hook.sh
```

## Building & Testing

Run JVM tests and lint:

```bash
./gradlew spotlessCheck :server:test :shared:jvmTest :shared:testDebugUnitTest :app:jvmTest :app:testDebugUnitTest
```

Run all tests including Android and iOS (requires platform tools):

```bash
./gradlew check
```

## Running

### Server

Start containerized database and the Ktor server (skip servers if you want to manage them manually):

```bash
./gradlew :server:postgres :server:run
```

### Android

Build and install the debug APK:

```bash
./gradlew :app:assembleDebug
```

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run the scheme, or build the framework manually:

```bash
./gradlew :app:linkDebugFrameworkIosSimulatorArm64
```

### Server + Desktop app together

```bash
./run_dev.sh
```

This starts the server (with database) and the desktop JVM app in parallel.
