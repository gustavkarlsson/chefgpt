#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SOURCE_FILE="$SCRIPT_DIR/spotless-pre-commit-hook.txt"
START_MARKER="######## SPOTLESS-GRADLE HOOK START ########"
END_MARKER="######## SPOTLESS-GRADLE HOOK END ########"

if [ ! -f "$SOURCE_FILE" ]; then
    echo "Error: $SOURCE_FILE not found."
    exit 1
fi

# Hooks are resolved from the common git dir, which is shared by all worktrees.
# Deriving it from git also avoids assuming ".git" is a directory.
GIT_COMMON_DIR="$(git -C "$SCRIPT_DIR" rev-parse --path-format=absolute --git-common-dir 2>/dev/null)"
if [ -z "$GIT_COMMON_DIR" ]; then
    echo "Error: $SCRIPT_DIR is not inside a git repository."
    exit 1
fi

HOOK_DIR="$GIT_COMMON_DIR/hooks"
HOOK_FILE="$HOOK_DIR/pre-commit"

mkdir -p "$HOOK_DIR"

if [ -f "$HOOK_FILE" ]; then
    if grep -qF "$START_MARKER" "$HOOK_FILE"; then
        echo "Updating existing spotless hook block..."
        tmp_file="$(mktemp)"
        awk -v start="$START_MARKER" -v end="$END_MARKER" -v src="$SOURCE_FILE" '
            $0 == start { skip = 1; while ((getline line < src) > 0) print line; close(src); next }
            $0 == end   { skip = 0; next }
            !skip
        ' "$HOOK_FILE" > "$tmp_file" && cat "$tmp_file" > "$HOOK_FILE"
        rm -f "$tmp_file"
    else
        echo "Appending to existing pre-commit hook..."
        echo "" >> "$HOOK_FILE"
        cat "$SOURCE_FILE" >> "$HOOK_FILE"
    fi
else
    echo "Creating new pre-commit hook..."
    echo "#!/bin/sh" > "$HOOK_FILE"
    cat "$SOURCE_FILE" >> "$HOOK_FILE"
fi

chmod +x "$HOOK_FILE"
echo "Successfully installed spotless pre-commit hook."
