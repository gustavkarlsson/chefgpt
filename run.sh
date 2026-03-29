#!/usr/bin/env bash
set -e

trap 'kill 0' SIGINT SIGTERM
./gradlew :server:postgres :server:run &
./gradlew :app:jvmRun &
wait
