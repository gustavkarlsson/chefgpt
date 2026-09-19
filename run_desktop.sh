#!/usr/bin/env bash
set -euo pipefail

# Build and run the desktop JVM app with Compose hot reload. `--auto` reloads
# the app whenever sources change.
./gradlew :app:hotRunJvm --auto
