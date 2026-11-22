#!/usr/bin/env bash
# Build runnable Android UI + Java ME core. Go AAR is optional.
# Requires JDK17, Gradle 8.9, Android SDK 35. No Go/NDK/gomobile required.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
for cmd in java javac gradle; do command -v "$cmd" >/dev/null || { echo "Missing $cmd" >&2; exit 2; }; done
[[ -n "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]] || { echo "Missing ANDROID_HOME" >&2; exit 2; }
"$ROOT/tools/verify_legacy.sh"
cd "$ROOT/android"
if [[ ! -f gradle/wrapper/gradle-wrapper.jar ]]; then gradle wrapper --gradle-version 8.9; fi
./gradlew --no-daemon --stacktrace assembleDebug
printf '\nOutput: %s\n' "$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
