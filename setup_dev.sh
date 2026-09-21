#!/bin/bash
set -e

SOURCE="server/application_dev_template.conf"
TARGET="server/application_dev.conf"

if [ -f "$TARGET" ]; then
    echo "$TARGET already exists, skipping."
    exit 0
fi

# A fresh worktree does not have the untracked dev config. Reuse the one from
# the repository's main checkout when it exists, instead of the template.
MAIN_ROOT=$(git worktree list --porcelain 2>/dev/null | awk '/^worktree / { print $2; exit }')
if [ -n "$MAIN_ROOT" ]; then
    MAIN_CONF="$MAIN_ROOT/server/application_dev.conf"
    if [ -f "$MAIN_CONF" ]; then
        cp "$MAIN_CONF" "$TARGET"
        echo "Copied $MAIN_CONF to $TARGET."
        exit 0
    fi
fi

cp "$SOURCE" "$TARGET"
echo "Created $TARGET from template."
