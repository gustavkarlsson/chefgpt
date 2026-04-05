---
name: snapshot-test
description: Write snapshot tests for Ktor HTTP routes. Use when adding, updating, or debugging snapshot tests for server endpoints.
---

Snapshot tests in this project capture the full HTTP request+response (including headers and JSON body) and compare against a stored JSON file on each run.
They live in `server/src/test/kotlin/.../` and their snapshots in `server/src/test/snapshots/`.
In contrast to normal unit tests, snapshot tests run with the Junit5 API.

## Test structure

```kotlin
@ExtendWith(SnapshotExtension::class)
class MyFeatureSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `my scenario`() =
        snapshotTestApplication(snapshotContext) { client ->
            // set up state, then make the request to snapshot
            client.get("/my-endpoint") {
                header("Session-Id", sessionId)
            }
        }
}
```

`snapshotTestApplication()` (defined in `SnapshotTestHelpers.kt`) wraps the Ktor test engine, installs the snapshot plugin, and sanitizes dynamic values (UUIDs, timestamps, session IDs, Content-Length) by replacing them with stable placeholders before writing the snapshot.

Use separate clients when setting up state (e.g. register a user, create a chat) without those requests being captured.

## Snapshot files

Each test method gets its own file:
```
server/src/test/snapshots/se/gustavkarlsson/chefgpt/{TestClassName}/{test name()}.json
```

<!-- TODO Remove stuff about dynamic values when that's been solved. -->
The file captures the full request and response. Dynamic values are replaced with placeholders (`00000000-0000-0000-0000-000000000000`, `2000-01-01T00:00:00Z`, etc.) so snapshots stay stable across runs.

## Workflow

**Running tests normally** (verify against existing snapshots):
```bash
./gradlew :server:test
```

**New test — snapshot doesn't exist yet:** The test will fail on first run. This is normal.
Review the generated JSON file to confirm the captured response looks correct before committing it.

**Intentional change to a route's response:** Update the snapshot the same way — run with `-PsnapshotAction=overwrite`, then review the diff.

**After deleting or renaming tests:** Clean up orphaned snapshot files:
```bash
./gradlew :server:clearSnapshots
```

## What to commit

Always commit snapshot files alongside the test code that produces them. A snapshot file is the expected output — it's part of the test.
