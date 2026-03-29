#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
HOOK_DIR="$SCRIPT_DIR/.git/hooks"
HOOK_FILE="$HOOK_DIR/pre-commit"
SOURCE_FILE="$SCRIPT_DIR/spotless-pre-commit-hook.txt"

if [ ! -f "$SOURCE_FILE" ]; then
    echo "Error: $SOURCE_FILE not found."
    exit 1
fi

mkdir -p "$HOOK_DIR"

if [ -f "$HOOK_FILE" ]; then
    if grep -q "######## SPOTLESS-GRADLE HOOK START ########" "$HOOK_FILE"; then
        echo "Spotless hook already exists in $HOOK_FILE. Skipping."
        exit 0
    fi
    echo "Appending to existing pre-commit hook..."
    echo "" >> "$HOOK_FILE"
    cat "$SOURCE_FILE" >> "$HOOK_FILE"
else
    echo "Creating new pre-commit hook..."
    echo "#!/bin/sh" > "$HOOK_FILE"
    cat "$SOURCE_FILE" >> "$HOOK_FILE"
fi

chmod +x "$HOOK_FILE"
echo "Successfully installed spotless pre-commit hook."
