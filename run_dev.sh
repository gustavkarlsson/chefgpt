#!/usr/bin/env bash
set -e

cleanup() {
    echo "Stopping..."
    kill -- -$$ 2>/dev/null
}
trap cleanup SIGINT SIGTERM

# Run server (with database) and app in parallel
./gradlew :server:postgres :server:run &
./gradlew :app:run &

wait
