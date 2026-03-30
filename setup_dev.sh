#!/bin/bash
set -e

TARGET="server/application_dev.conf"

if [ -f "$TARGET" ]; then
    echo "$TARGET already exists, skipping."
else
    cp application_dev_template.conf "$TARGET"
    echo "Created $TARGET from template."
fi
