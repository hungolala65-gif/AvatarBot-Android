#!/usr/bin/env bash
# Build a genuine Android APK for v0.18 and verify it before publishing.
# Requires installed JDK 17, Android SDK 35, and Gradle 8.9 or existing wrapper.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP="$ROOT/android"
OUT="$ROOT/dist"
APK="$APP/app/build/outputs/apk/debug/app-debug.apk"
mkdir -p "$OUT"
if [[ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]]; then
  echo 'ERROR: Android SDK is missing. Install Android SDK platform 35 and build-tools 35.0.0; set ANDROID_HOME.' >&2
  exit 20
fi
SDK="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
if [[ ! -s "$SDK/platforms/android-35/android.jar" ]]; then
  echo "ERROR: $SDK/platforms/android-35/android.jar is missing (install SDK Platform 35)." >&2
  exit 21
fi
if [[ -x "$APP/gradlew" ]]; then
  GRADLE=("$APP/gradlew")
elif command -v gradle >/dev/null 2>&1; then
  echo 'Creating local Gradle wrapper version 8.9...'
  (cd "$APP" && gradle wrapper --gradle-version 8.9)
  GRADLE=("$APP/gradlew")
else
  echo 'ERROR: Gradle is missing. Install Gradle 8.9 or generate the Android Gradle wrapper.' >&2
  exit 22
fi
bash "$ROOT/tools/verify_legacy.sh"
(cd "$ROOT/go-mobilecore" && go test ./...)
(cd "$APP" && "${GRADLE[@]}" --no-daemon --stacktrace assembleDebug)
[[ -s "$APK" ]] || { echo "ERROR: Missing APK $APK" >&2; exit 23; }
cp "$APK" "$OUT/AvatarBot-Android-v0.18-debug.apk"
(cd "$OUT" && sha256sum AvatarBot-Android-v0.18-debug.apk > AvatarBot-Android-v0.18-debug.apk.sha256)
python3 - "$OUT/AvatarBot-Android-v0.18-debug.apk" <<'PY'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as apk:
    assert apk.testzip() is None, 'APK ZIP corrupted'
    names=set(apk.namelist())
    for item in ('AndroidManifest.xml','classes.dex','resources.arsc'):
        assert item in names, f'Missing required APK part {item}'
print('PASS: APK ZIP verified')
PY
printf 'BUILD SUCCESS. APK: %s\n' "$OUT/AvatarBot-Android-v0.18-debug.apk"
