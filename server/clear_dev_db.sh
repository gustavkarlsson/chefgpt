#!/usr/bin/env bash
# Stops and removes the dockerized dev Postgres container.
#
# The container's data is not volume-mounted, so removing it clears the
# database. The next `./gradlew :server:postgres` recreates it empty.
set -euo pipefail

CONTAINER_NAME="chefgpt-postgres"

if ! command -v docker >/dev/null 2>&1; then
    echo "Error: docker is not installed." >&2
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "Error: cannot reach the Docker daemon." >&2
    exit 1
fi

if docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
    echo "Stopping and removing container ${CONTAINER_NAME}..."
    docker rm -f "$CONTAINER_NAME"
    echo "Removed ${CONTAINER_NAME}. Run ./gradlew :server:postgres to recreate it."
else
    echo "Container ${CONTAINER_NAME} does not exist; nothing to remove."
fi
