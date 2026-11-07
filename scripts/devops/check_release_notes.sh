#!/usr/bin/env bash
set -euo pipefail

if [ -f "docs/LATEST_RELEASE.md" ]; then
    NOTES_FILE="docs/LATEST_RELEASE.md"
    echo "Found release notes at $NOTES_FILE"
else
    NOTES_FILE=""
    echo "No release notes found at docs/LATEST_RELEASE.md"
fi

if [ -n "${GITHUB_OUTPUT:-}" ]; then
    echo "notes_file=$NOTES_FILE" >> "$GITHUB_OUTPUT"
fi
