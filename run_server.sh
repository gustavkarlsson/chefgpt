#!/usr/bin/env bash
set -euo pipefail

# Start the containerized Postgres database, then the Ktor server.
# `:server:run` is ordered after `:server:postgres`, so the database is ready
# before the server connects to it.
./gradlew :server:postgres :server:run
