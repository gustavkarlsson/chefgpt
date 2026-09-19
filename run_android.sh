#!/usr/bin/env bash
set -euo pipefail

# Build the debug APK, then install and launch it on the connected device.
./gradlew :androidApp:assembleDebug
android run --apks androidApp/build/outputs/apk/debug/androidApp-debug.apk
