#!/usr/bin/env bash
set -euo pipefail

TAG_NAME="${TAG_NAME:-v1.0.0}"
mkdir -p release_assets

APK_PATH=$(find app/build/outputs/apk/release -name "*.apk" | head -n 1 || true)

if [ -n "$APK_PATH" ] && [ -f "$APK_PATH" ]; then
    OUTPUT_NAME="release_assets/MobileCroaker-${TAG_NAME}.apk"
    cp "$APK_PATH" "$OUTPUT_NAME"
    echo "Packaged APK: $OUTPUT_NAME"
    if [ -n "${GITHUB_OUTPUT:-}" ]; then
        echo "apk_path=$OUTPUT_NAME" >> "$GITHUB_OUTPUT"
    fi
else
    echo "Error: No release APK found in app/build/outputs/apk/release"
    exit 1
fi
