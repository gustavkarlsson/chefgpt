#!/usr/bin/env bash
set -euo pipefail

# Build the debug app, then install and launch it on a connected iPhone, falling
# back to a simulator that is booted, and created first if the machine has none.
# Gradle has no task for this: it only builds the Kotlin framework, which Xcode
# already invokes through the project's "Compile Kotlin Framework" build phase.

# A device type name, as listed by `xcrun simctl list devicetypes`. Stock
# simulators are named after their device type, so this reuses an existing one
# and otherwise creates it. simctl takes the name wherever it takes a udid.
SIMULATOR_DEVICE="${SIMULATOR_DEVICE:-iPhone 17 Pro}"
DERIVED_DATA="build/ios"

# devicectl reports a connected device's udid in the identifier column.
UUID='[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}'
DEVICE="$(xcrun devicectl list devices 2>/dev/null | grep connected | grep -oE "$UUID" | head -1 || true)"

if [ -n "$DEVICE" ]; then
    grep -q '^TEAM_ID=.\+' iosApp/Configuration/Config.xcconfig ||
        echo "Note: TEAM_ID is empty in iosApp/Configuration/Config.xcconfig; signing will fail." >&2
    DESTINATION="id=$DEVICE"
else
    xcrun simctl list devices available | grep -q " $SIMULATOR_DEVICE (" ||
        xcrun simctl create "$SIMULATOR_DEVICE" "$SIMULATOR_DEVICE"
    xcrun simctl boot "$SIMULATOR_DEVICE" 2>/dev/null || true
    open -a Simulator
    xcrun simctl bootstatus "$SIMULATOR_DEVICE" -b
    DESTINATION="platform=iOS Simulator,name=$SIMULATOR_DEVICE"
fi

xcodebuild build \
    -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -destination "$DESTINATION" \
    -derivedDataPath "$DERIVED_DATA"

# The bundle id is built from TEAM_ID, so read it back rather than hardcoding it.
APP="$(find "$DERIVED_DATA/Build/Products" -maxdepth 2 -name '*.app' -print -quit)"
BUNDLE_ID="$(plutil -extract CFBundleIdentifier raw "$APP/Info.plist")"

if [ -n "$DEVICE" ]; then
    xcrun devicectl device install app --device "$DEVICE" "$APP"
    xcrun devicectl device process launch --device "$DEVICE" "$BUNDLE_ID"
else
    xcrun simctl install "$SIMULATOR_DEVICE" "$APP"
    xcrun simctl launch "$SIMULATOR_DEVICE" "$BUNDLE_ID"
fi
