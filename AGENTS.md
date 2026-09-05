# Coding Agent guidelines

## Repository Layout
- `server`: Ktor backend
- `app`: Kotlin Multiplatform client project — all client code, every target
- `androidApp`: thin Android application wrapper around `app` (manifest, resources, `MainActivity`)
- `shared`: Kotlin Multiplatform shared code between server and clients
- `iosApp`: XCode project for the iOS client

## Dependencies

To upgrade dependencies, use the update-dependencies skill.

## Testing

Run JVM tests (CI-equivalent, fast):
```bash
./gradlew spotlessCheck :server:test :shared:jvmTest :shared:testAndroidHostTest :app:jvmTest :app:testAndroidHostTest
```

Run all tests and lint checks (slow and requires platform-specific tools installed):
```bash
./gradlew check
```

Individual modules can also be tested if changes are isolated.

## Environment

If a build fails for toolchain reasons (JDK, Android SDK, Xcode, Docker), use the setup skill.

## Completing a task

Before considering a task as done, use the verify skill to find any issues.

## Commit messages

Follow: `<title><empty line><body>`.

Keep every line at most 72 characters.

Example:
```
Derezz the master control program

MCP turned out to be evil and had become intent on world domination.
This commit throws Tron's disc into MCP (causing its deresolution)
and turns it back into a chess game.
```

## Pull requests

Every PR should answer: what changed, why, and any breaking changes.
