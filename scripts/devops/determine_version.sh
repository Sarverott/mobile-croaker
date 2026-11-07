#!/usr/bin/env bash
set -euo pipefail

# Determine Release Version & Tag
if [[ "${GITHUB_REF:-}" == refs/tags/* ]]; then
    RAW_TAG="${GITHUB_REF#refs/tags/}"
    TAG_NAME="$RAW_TAG"
    VERSION="${RAW_TAG#v}"
elif [ -f "package.json" ]; then
    VERSION=$(node -p "require('./package.json').version" 2>/dev/null || echo "1.0.0")
    TAG_NAME="v$VERSION"
elif [ -f "pyproject.toml" ]; then
    VERSION=$(grep -m 1 '^version = ' pyproject.toml | cut -d '"' -f 2 || echo "1.0.0")
    TAG_NAME="v$VERSION"
else
    VERSION="1.0.0"
    TAG_NAME="v$VERSION"
fi

echo "Detected version: $VERSION (Tag: $TAG_NAME)"

if [ -n "${GITHUB_ENV:-}" ]; then
    echo "VERSION=$VERSION" >> "$GITHUB_ENV"
    echo "TAG_NAME=$TAG_NAME" >> "$GITHUB_ENV"
fi
