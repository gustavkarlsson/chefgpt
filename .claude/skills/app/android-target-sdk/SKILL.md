---
name: android-target-sdk
description: Raise the Android `targetSdk` to a new API version, reviewing the platform behavior changes it opts the app into. Use when upgrading the Android target version or when a newer target API is required.
---

`targetSdk` is set once in `gradle/libs.versions.toml` as `androidTargetSdk`. Raising it opts the app into every behavior change gated behind each API version in between, so treat this as a behavioral change, not a version bump.

---

## 1. Establish the range

Read the current `androidTargetSdk` from `gradle/libs.versions.toml`. If the user didn't name a destination version, ask.

Every version in `(current, new]` must be reviewed — a multi-version jump reviews each one, not just the last.

---

## 2. Check `compileSdk`

`compileSdk` must be at least `targetSdk`. If `androidCompileSdk` is lower than the new target, stop and use the **update-dependencies** skill, scoping it to `androidCompileSdk` plus whatever that forces — installing the SDK package, and the Gradle and AGP bumps it may require. That skill owns `compileSdk`; don't bump it by hand and don't upgrade anything else as a side effect of this task.

The split is deliberate and reciprocal: **update-dependencies** treats `androidTargetSdk` as a hard constraint it must never touch, because opting into new OS behavior is a product decision. That decision is this skill's job.

Land the `compileSdk` change and its verification before starting on the target bump, so a failure belongs to one or the other.

---

## 3. Read the behavior changes

The docs are keyed by **Android platform version**, not by API version — API 36 is Android 16. Look up the platform version for each API version in the range at `https://developer.android.com/tools/releases/platforms`, then fetch both pages for each one, where `<V>` is the platform version:

- `https://developer.android.com/about/versions/<V>/behavior-changes-<V>` — changes that apply to apps *targeting* that version. These are the ones this bump turns on.
- `https://developer.android.com/about/versions/<V>/behavior-changes-all` — changes that apply to all apps running on that version.

If a URL 404s, search for the current location rather than assuming the page doesn't exist.

**Never fill this in from memory.** These pages routinely post-date the model's knowledge cutoff. If the docs can't be reached, stop and tell the user rather than proceeding on guesses.

---

## 4. Triage each change against this codebase

Only Android-specific code can be affected: the Android source sets, manifest, resources and Gradle configuration, plus any Android or AndroidX API reached from shared code.

For each documented change, identify the API or capability it affects and search for that specific thing. A change is only relevant if the app actually uses what it governs — don't reason from the change's title.

Bucket each change as one of:

- **Not applicable** — the app doesn't use the affected API or capability.
- **Applies, mechanical** — config, manifest or annotation only, with no user-visible difference.
- **Applies, changes app behavior** — anything a user could notice, or that could fail at runtime.

---

## 5. Ask before anything behavioral

This is the gate. For every change in the third bucket, and anything ambiguous, **ask the user** before touching it. State plainly:

- what the platform does differently once the app targets the new version
- what breaks or changes for users if nothing is done
- the options, with a recommendation

Never silently apply a behavioral change, and never add unrelated refactors while in here.

---

## 6. Apply

Bump `androidTargetSdk` in `gradle/libs.versions.toml` — version catalog only, never inline literals in build files — plus the changes agreed in step 5.

---

## 7. Verify

```bash
./gradlew lintDebug
./gradlew :androidApp:assembleDebug
```

`lintDebug` is the CI gate (`.github/workflows/verify.yml`). Then run the **verify** skill.

A green build proves little here. Nothing in the toolchain rejects a `targetSdk` above `compileSdk`, and no automated check exercises the behavior changes — confirm by reading that `androidCompileSdk` is still at least `androidTargetSdk`, and treat everything from step 5 as needing manual testing.

---

Report: old and new API version, every behavior change reviewed with its bucket and the decision taken, and — since the repo has no Android instrumentation tests — an explicit list of what still needs manual testing on a device or emulator.
