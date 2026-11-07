#!/usr/bin/env bash
set -euo pipefail

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
else
    echo "Gradle wrapper not found, generating via gradle wrapper..."
    gradle wrapper
    chmod +x ./gradlew
fi

echo "Building Release APK with Gradle..."
./gradlew assembleRelease --no-daemon --stacktrace
