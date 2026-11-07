#!/usr/bin/env bash
set -euo pipefail

WORKSPACE="${GITHUB_WORKSPACE:-$(pwd)}"
KEYSTORE_PATH="$WORKSPACE/my-upload-key.jks"

if [ -n "${KEYSTORE_BASE64:-}" ]; then
    echo "Using base64 keystore from secrets..."
    echo "$KEYSTORE_BASE64" | base64 -d > "$KEYSTORE_PATH"
    STORE_PASS="${STORE_PASSWORD:-android}"
    KEY_PASS="${KEY_PASSWORD:-android}"
elif [ -f "debug.keystore.base64" ]; then
    echo "Decoding repository debug.keystore.base64 for release signing..."
    base64 -d debug.keystore.base64 > "$KEYSTORE_PATH"
    STORE_PASS="android"
    KEY_PASS="android"
else
    echo "Generating fallback self-signed upload keystore..."
    keytool -genkey -v -keystore "$KEYSTORE_PATH" \
      -alias upload -keyalg RSA -keysize 2048 -validity 10000 \
      -storepass "android" -keypass "android" \
      -dname "CN=MobileCroaker, O=Apokryf, C=PL"
    STORE_PASS="android"
    KEY_PASS="android"
fi

echo "Keystore configured at $KEYSTORE_PATH"

if [ -n "${GITHUB_ENV:-}" ]; then
    echo "KEYSTORE_PATH=$KEYSTORE_PATH" >> "$GITHUB_ENV"
    echo "STORE_PASSWORD=$STORE_PASS" >> "$GITHUB_ENV"
    echo "KEY_PASSWORD=$KEY_PASS" >> "$GITHUB_ENV"
fi
