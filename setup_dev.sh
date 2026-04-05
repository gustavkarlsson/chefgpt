#!/bin/bash
set -e

SOURCE="server/application_dev_template.conf"
TARGET="server/application_dev.conf"

if [ -f "$TARGET" ]; then
    echo "$TARGET already exists, skipping."
else
    cp "$SOURCE" "$TARGET"
    echo "Created $TARGET from template."
fi
