---
name: verify
description: Run the full CI-equivalent check suite (spotlessCheck + all JVM tests). Use before marking a task done or before opening a PR.
---

Run the following command and report the results:

```bash
./gradlew spotlessCheck :server:test :shared:jvmTest :shared:testDebugUnitTest :app:jvmTest :app:testDebugUnitTest
```

If `spotlessCheck` fails, run `./gradlew spotlessApply` first and then retry the full command.

If any server tests fail due to missing snapshots (new routes), rerun `./gradlew :server:test`. New snapshots should now exist.

Report: total tests run, failures (with test names and error messages), new snapshots, and whether formatting passed.
