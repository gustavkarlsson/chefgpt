---
name: android-min-sdk
description: Raise the Android `minSdk` to a new API version and remove the code and resources that only existed for older versions. Use when dropping support for older Android versions.
---

`minSdk` is set once in `gradle/libs.versions.toml` as `androidMinSdk` and consumed by both `app/build.gradle.kts` and `shared/build.gradle.kts`, so there's no per-module edit.

Raising it drops users on older devices. If the user didn't name a version, ask which one before changing anything.

---

## 1. Bump the version

Change `androidMinSdk` in `gradle/libs.versions.toml`.

---

## 2. Simplify what is now unconditional

Find the candidates:

```bash
rg -n 'SDK_INT|RequiresApi|TargetApi|ChecksSdkIntAtLeast|VERSION_CODES|maxSdkVersion' app shared
find app/src shared/src -type d -name '*-v[0-9]*'
```

Then:

- **Version checks** — an `SDK_INT` comparison against a version at or below the new `minSdk` is always true. Delete the check and the legacy branch, keeping only the modern one. A check *above* the new min stays. A read of `SDK_INT` that isn't a comparison is data, not a guard — leave it alone.
- **Annotations** — delete `@RequiresApi`, `@TargetApi` and `@ChecksSdkIntAtLeast` at or below the new min.
- **Resource qualifier directories** — for each `-v<N>` directory where `N` is at or below the new min, drop the `-v<N>` segment and keep every other qualifier: `drawable-v24` folds into `drawable`, but `mipmap-anydpi-v26` folds into `mipmap-anydpi`, *not* `mipmap`. Losing the other qualifiers changes which resource the system picks. If the destination folder already holds a file of the same name, merge the contents rather than overwriting — folding a `values-v31` theme attribute means merging it into `values/themes.xml`, and `values-night/` may need the same edit.
- **Manifest** — drop `android:maxSdkVersion` guards on permissions that can no longer apply.
- **Compat shims** — remove a library or helper kept solely for older versions only when it is clearly redundant. Flag the rest for the user instead of churning code.

---

## 3. Verify

```bash
./gradlew lintDebug
./gradlew :app:assembleDebug
```

`lintDebug` also catches the inverse mistake — code still guarding a version below the new floor. Then run the **verify** skill.

---

Report: old and new API version, each simplification made, and anything flagged but deliberately left alone.
