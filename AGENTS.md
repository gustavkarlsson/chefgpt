# Coding Agent guidelines

## Repository Layout
- `server`: Ktor backend
- `app`: Kotlin Multiplatform client project
- `shared`: Kotlin Multiplatform shared code between server and clients
- `iosApp`: XCode project for the iOS client

## General Guidance
- Use modern Kotlin language features when possible.
- Don't write more code than necessary to solve the problem.
- Lean towards functional programming patterns and avoid mutable data.
- Only comment the code when its purpose or implementation is unclear.
- Never use fully qualified references in code. Use imports instead (and typealiases when collisions occur).

## Dependencies
Dependencies are always defined using version catalogs. The version should always be a version.ref.

## Code style
`.editorconfig` contains some rules for formatting, encoding, indentation, etc.

Kotlin formatting is enforced by the [Spotless plugin](https://github.com/diffplug/spotless):
```bash
./gradlew spotlessApply
```

## Testing
Before running any tests, always format the code with spotless
to ensure that the code is formatted correctly (which warms up build caches).

### Writing tests
The structure of unit tests should follow the "Arrange-Act-Assert" pattern.
Add a blank line between each section to visualize it, otherwise avoid blank lines

Assertions should be written using [strikt](https://strikt.io/).
Try to avoid multiple assertions in a single test. If necessary, use the Strikt's "soft assertions" like this:
```kotlin
expect {
    that(actual).isEqualTo(expected)
    that(actualList).contains(expectedElement)
}
```

### Snapshot testing (server)
The server uses [slapshot](https://github.com/gustavkarlsson/slapshot) with the ktor3 integration for snapshot testing HTTP routes. Snapshots are stored in `server/src/test/snapshots/`.

Use `RoutesSnapshotTest` as a reference for writing new snapshot tests. The pattern is:
1. Set up the test class with `@ExtendWith(SnapshotExtension::class)` and store `snapshotContext` via `@BeforeEach`.
2. In each test, call `testApplication` with the shared `testModule()` setup.
3. Create a client with `install(SnapshotTesting(snapshotContext))` and make HTTP requests. Slapshot automatically snapshots each request/response pair.

The first run of a new snapshot test always fails and creates the snapshot file. The second run compares against it.

#### Overwriting snapshots
When a snapshot change is intentional (e.g. you changed response format), overwrite existing snapshots:
```bash
./gradlew :server:test -PsnapshotAction=overwrite
```
Review the updated snapshot files before committing.

#### Clearing snapshots
When test cases are removed, clean up orphaned snapshot files:
```bash
./gradlew :server:clearSnapshots
```
Then re-run tests to regenerate only the snapshots that are still needed.

### Running tests
Tests can take a long time in a Kotlin Multiplatform project. Therefore, there are two approaches for testing.

### Run only JVM tests (fast)
```bash
./gradlew :server:test :shared:jvmTest :shared:testDebugUnitTest :app:jvmTest :app:testDebugUnitTest
```

### Run all tests (slow)
```bash
./gradlew test
```

## Completing a task
Before an implementation task can be considered completed, ensure that:
- New code is covered by tests
- Code has been formatted
- JVM tests pass

## Commit Messages
Follow the [Chris Beams](http://chris.beams.io/posts/git-commit/) style for commit messages.

## Pull Requests
Every pull request should answer:
- What changed?
- Why?
- Breaking changes?
