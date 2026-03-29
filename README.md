This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM).

* [/app](./app/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./app/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./app/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./app/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :app:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :app:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :app:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :app:run
  ```

### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:

- for the Wasm target (faster, modern browsers):
    - on macOS/Linux
      ```shell
      ./gradlew :app:wasmJsBrowserDevelopmentRun
      ```
    - on Windows
      ```shell
      .\gradlew.bat :app:wasmJsBrowserDevelopmentRun
      ```
- for the JS target (slower, supports older browsers):
    - on macOS/Linux
      ```shell
      ./gradlew :app:jsBrowserDevelopmentRun
      ```
    - on Windows
      ```shell
      .\gradlew.bat :app:jsBrowserDevelopmentRun
      ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Snapshot Testing (Server)

The server uses [slapshot](https://github.com/gustavkarlsson/slapshot) with the ktor3 integration for snapshot testing HTTP routes. Snapshots are stored in `server/src/test/snapshots/`.

Each endpoint has its own snapshot test file (e.g. `RegisterSnapshotTest`). The first run of a new snapshot test always fails and creates the snapshot file. The second run compares against it.

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

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).