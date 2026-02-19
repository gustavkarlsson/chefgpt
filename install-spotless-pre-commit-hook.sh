#!/bin/bash

HOOK_FILE=".git/hooks/pre-commit"
SOURCE_FILE="spotless-pre-commit-hook.txt"

if [ ! -f "$SOURCE_FILE" ]; then
    echo "Error: $SOURCE_FILE not found."
    exit 1
fi

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
