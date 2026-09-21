#!/bin/sh
set -eu

# Herdr runs event hooks with the plugin directory as the working directory and
# passes the invocation context as JSON in HERDR_PLUGIN_CONTEXT_JSON.
context="${HERDR_PLUGIN_CONTEXT_JSON:-}"

if [ -z "$context" ]; then
    echo "chefgpt.worktree-cleanup: HERDR_PLUGIN_CONTEXT_JSON is unset; nothing to do" >&2
    exit 0
fi

if ! command -v jq >/dev/null 2>&1; then
    echo "chefgpt.worktree-cleanup: jq is required but not found" >&2
    exit 1
fi

checkout=$(printf '%s' "$context" | jq -r '.worktree.checkout_path // empty')

if [ -z "$checkout" ]; then
    echo "chefgpt.worktree-cleanup: closed workspace is not a worktree; nothing to do"
    exit 0
fi

# A removed worktree (as opposed to a closed one) is already gone.
if [ ! -d "$checkout" ]; then
    echo "chefgpt.worktree-cleanup: $checkout no longer exists; nothing to do"
    exit 0
fi

if [ ! -x "$checkout/gradlew" ]; then
    echo "chefgpt.worktree-cleanup: $checkout/gradlew not found; nothing to do" >&2
    exit 0
fi

echo "chefgpt.worktree-cleanup: running ./gradlew clean in $checkout"
(cd "$checkout" && ./gradlew clean)
