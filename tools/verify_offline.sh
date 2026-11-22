#!/usr/bin/env bash
# Works on a development PC without Android SDK (requires Go and Kotlin CLI).
# Does not access any real game server, accounts, or credentials.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
(
  cd "$ROOT/go-mobilecore"
  go test -race ./...
  go vet ./...
  CGO_ENABLED=0 GOOS=android GOARCH=arm64 go build ./...
)
if command -v kotlinc >/dev/null 2>&1; then
  tmpdir="$(mktemp -d)"
  trap 'rm -rf "$tmpdir"' EXIT
  kotlinc "$ROOT/android/app/src/main/java/com/avatarbot/mobile/data/AccountImporter.kt" \
    "$ROOT/tools/verify_importer.kt" -include-runtime -d "$tmpdir/importer.jar"
  java -jar "$tmpdir/importer.jar"
  kotlinc "$ROOT/android/app/src/main/java/com/avatarbot/mobile/data/GameCities.kt" \
    "$ROOT/tools/verify_cities.kt" -include-runtime -d "$tmpdir/cities.jar"
  java -jar "$tmpdir/cities.jar"
  kotlinc "$ROOT/android/app/src/main/java/com/avatarbot/mobile/data/DiagnosticRedactor.kt" \
    "$ROOT/tools/verify_diagnostic.kt" -include-runtime -d "$tmpdir/diagnostics.jar"
  java -jar "$tmpdir/diagnostics.jar"
  kotlinc "$ROOT/tools/jvmstubs/JSONObject.kt" \
    "$ROOT/android/app/src/main/java/com/avatarbot/mobile/engine/KotlinNetworkProbe.kt" \
    "$ROOT/tools/verify_network.kt" -include-runtime -d "$tmpdir/network.jar"
  java -jar "$tmpdir/network.jar"
  KDIR="$(cd "$(dirname "$(command -v kotlinc)")/../lib" && pwd)"
  if [[ -f "$KDIR/kotlin-compiler.jar" ]]; then
    kotlinc -cp "$KDIR/kotlin-compiler.jar" "$ROOT/tools/verify_kotlin_psi.kt" -d "$tmpdir/psi.jar"
    kotlin -cp "$tmpdir/psi.jar:$KDIR/kotlin-compiler.jar" Verify_kotlin_psiKt       "$ROOT/android/app/src/main/java"
  else
    echo 'NOTE: kotlin-compiler.jar missing; Kotlin PSI syntax check skipped' >&2
  fi
else
  echo 'NOTE: kotlinc not installed; Kotlin JVM tests skipped' >&2
fi
python3 "$ROOT/tools/verify_session_source.py"
python3 "$ROOT/tools/verify_auth_guard_source.py"
"$ROOT/tools/verify_setup_options_v15.sh"
"$ROOT/tools/verify_legacy.sh"
printf 'PASS: all available offline checks completed\n'
