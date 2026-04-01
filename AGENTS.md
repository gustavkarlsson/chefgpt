# Coding Agent guidelines

## Repository Layout
- `server`: Ktor backend
- `app`: Kotlin Multiplatform client project
- `shared`: Kotlin Multiplatform shared code between server and clients
- `iosApp`: XCode project for the iOS client

## General

- Use modern Kotlin language features.
- Don't write more code than necessary.
- Prefer functional patterns and immutable data.
- Comment only when purpose or implementation is unclear.
- Never use fully qualified references — use imports (and typealiases for collisions).

## Dependencies

Always use version catalogs. Versions must use `version.ref`, never inline literals.

## Testing

Run JVM tests (CI-equivalent, fast):
```bash
./gradlew spotlessCheck :server:test :shared:jvmTest :shared:testDebugUnitTest :app:jvmTest :app:testDebugUnitTest
```

Run all tests (slow):
```bash
./gradlew test
```

Individual modules can also be tested if changes are isolated.

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
