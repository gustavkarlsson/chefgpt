#!/bin/sh
set -eu

# Herdr runs event hooks with the plugin directory as the working directory and
# passes the invocation context as JSON in HERDR_PLUGIN_CONTEXT_JSON.
context="${HERDR_PLUGIN_CONTEXT_JSON:-}"

if [ -z "$context" ]; then
    echo "chefgpt.worktree-setup: HERDR_PLUGIN_CONTEXT_JSON is unset; nothing to do" >&2
    exit 0
fi

if ! command -v jq >/dev/null 2>&1; then
    echo "chefgpt.worktree-setup: jq is required but not found" >&2
    exit 1
fi

checkout=$(printf '%s' "$context" | jq -r '.worktree.checkout_path // empty')

if [ -z "$checkout" ]; then
    echo "chefgpt.worktree-setup: could not resolve the worktree checkout from context" >&2
    exit 0
fi

echo "chefgpt.worktree-setup: running setup_dev.sh in $checkout"
(cd "$checkout" && sh ./setup_dev.sh)
