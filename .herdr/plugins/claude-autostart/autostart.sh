#!/bin/sh
set -eu

# Herdr runs event hooks with the plugin directory as the working directory and
# passes the invocation context as JSON in HERDR_PLUGIN_CONTEXT_JSON, plus the
# full event envelope in HERDR_PLUGIN_EVENT_JSON.
context="${HERDR_PLUGIN_CONTEXT_JSON:-}"
event_json="${HERDR_PLUGIN_EVENT_JSON:-}"

if ! command -v jq >/dev/null 2>&1; then
    echo "chefgpt.worktree-claude-autostart: jq is required but not found" >&2
    exit 1
fi

herdr_bin="${HERDR_BIN_PATH:-herdr}"

workspace_id=$(printf '%s' "$context" | jq -r '.workspace_id // empty')
if [ -z "$workspace_id" ]; then
    echo "chefgpt.worktree-claude-autostart: no workspace in context; nothing to do" >&2
    exit 0
fi

# Reopening an already-open worktree must not start a second Claude.
already_open=$(printf '%s' "$event_json" | jq -r '.data.already_open // false')
if [ "$already_open" = "true" ]; then
    echo "chefgpt.worktree-claude-autostart: worktree already open; nothing to do"
    exit 0
fi

# Leave a pane alone if it already hosts an agent (e.g. Claude from an earlier
# open whose detection has not caught up with the guard above).
pane_agent=$(printf '%s' "$context" | jq -r '.focused_pane_agent // empty')
if [ -n "$pane_agent" ]; then
    echo "chefgpt.worktree-claude-autostart: pane already runs $pane_agent; nothing to do"
    exit 0
fi

pane_id=$(printf '%s' "$context" | jq -r '.focused_pane_id // empty')
if [ -z "$pane_id" ]; then
    pane_id=$("$herdr_bin" pane list --workspace "$workspace_id" 2>/dev/null \
        | jq -r '.result.panes[0].pane_id // empty' 2>/dev/null || true)
fi

if [ -z "$pane_id" ]; then
    echo "chefgpt.worktree-claude-autostart: no pane found for workspace $workspace_id" >&2
    exit 0
fi

# Name the agent after the branch so each worktree's Claude is distinct.
# Agent names must match [a-z][a-z0-9_-]{0,31}.
branch=$(printf '%s' "$event_json" | jq -r '.data.worktree.branch // empty')
name=""
if [ -n "$branch" ]; then
    name=$(printf '%s' "$branch" \
        | tr '[:upper:]' '[:lower:]' \
        | tr -cs 'a-z0-9_-' '-' \
        | sed 's/^-*//; s/-*$//' \
        | cut -c1-31)
    case "$name" in
        [a-z]*) ;;
        *) name="" ;;
    esac
fi
if [ -z "$name" ]; then
    name="claude-${workspace_id}"
fi

echo "chefgpt.worktree-claude-autostart: starting Claude in $pane_id as '$name'"

output=$("$herdr_bin" agent start "$name" --kind claude --pane "$pane_id" 2>&1)
status=$?
if [ "$status" -eq 0 ]; then
    echo "chefgpt.worktree-claude-autostart: Claude started"
    exit 0
fi

code=$(printf '%s' "$output" | jq -r '.error.code // empty' 2>/dev/null || true)
if [ "$code" = "agent_name_taken" ]; then
    echo "chefgpt.worktree-claude-autostart: agent '$name' already exists; nothing to do" >&2
    exit 0
fi

echo "chefgpt.worktree-claude-autostart: failed to start Claude: $output" >&2
exit 1
