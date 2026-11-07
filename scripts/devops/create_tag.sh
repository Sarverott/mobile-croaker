#!/usr/bin/env bash
set -euo pipefail

TAG_NAME="${TAG_NAME:-v1.0.0}"

git config user.name "github-actions[bot]"
git config user.email "github-actions[bot]@users.noreply.github.com"

if ! git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    echo "Creating tag $TAG_NAME..."
    git tag -a "$TAG_NAME" -m "Release $TAG_NAME"
    git push origin "$TAG_NAME" || echo "Tag push skipped or already exists remotely"
else
    echo "Tag $TAG_NAME already exists."
fi
