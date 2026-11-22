#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/go-mobilecore"
command -v go >/dev/null || { echo "Missing Go"; exit 1; }
command -v gomobile >/dev/null || { echo "Install gomobile: go install golang.org/x/mobile/cmd/gomobile@latest"; exit 1; }
[[ -n "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]] || { echo "ANDROID_HOME or ANDROID_SDK_ROOT is required"; exit 1; }
go test ./...
# New gomobile versions require x/mobile as a *module dependency*, not only
# a globally installed binary. Go >=1.24 is needed for the -tool directive.
if ! go list -m golang.org/x/mobile >/dev/null 2>&1; then
  go get -tool golang.org/x/mobile/cmd/gobind
fi
mkdir -p "$ROOT/android/app/libs"
gomobile init
gomobile bind -target=android/arm64 -androidapi=26 -o "$ROOT/android/app/libs/mobilecore.aar" .
echo "AAR created: $ROOT/android/app/libs/mobilecore.aar"
