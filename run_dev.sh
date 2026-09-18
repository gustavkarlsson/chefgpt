#!/usr/bin/env bash
set -euo pipefail

pids=()

cleanup() {
    # Runs on every exit path. Stops the Gradle clients we launched so the
    # server and app they forked via the daemon are torn down as well. Do not
    # use `kill -- -$$` here: that targets this shell's own process group,
    # re-entering the trap until bash's stack overflows and it segfaults.
    trap - INT TERM HUP
    echo "Stopping..."
    for pid in "${pids[@]:-}"; do
        kill -TERM "$pid" 2>/dev/null || true
    done
    wait 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
trap 'exit 129' HUP

# Run server (with database) and app in parallel
./gradlew :server:postgres :server:run &
pids+=("$!")

./gradlew :app:run &
pids+=("$!")

# Fail fast if either client exits. Bash 3.2 (macOS) has no `wait -n`, so poll
# until one of them is gone; the EXIT trap stops the other. Reap every client
# that has exited and propagate the first non-zero status, so a clean exit by
# one doesn't mask the other's failure.
while kill -0 "${pids[0]}" 2>/dev/null && kill -0 "${pids[1]}" 2>/dev/null; do
    sleep 1
done

status=0
for pid in "${pids[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
        continue
    fi
    code=0
    wait "$pid" || code=$?
    if [ "$code" -ne 0 ] && [ "$status" -eq 0 ]; then
        status=$code
    fi
done
exit "$status"
