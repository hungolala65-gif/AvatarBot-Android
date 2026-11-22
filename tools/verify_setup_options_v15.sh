#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
python3 - "$ROOT" "$tmp/AutoOptionsJvm.kt" <<'PY'
from pathlib import Path
import sys
source=Path(sys.argv[1],'android/app/src/main/java/com/avatarbot/mobile/data/SecureConfigStore.kt').read_text()
start=source.index('data class AutoOptions(')
end=source.index('\ndata class BotAccount(',start)
Path(sys.argv[2]).write_text('package com.avatarbot.mobile.data\nimport org.json.JSONObject\n\n'+source[start:end])
PY
kotlinc "$tmp/AutoOptionsJvm.kt" "$ROOT/tools/jvmstubs/JSONObjectOptions.kt" \
    "$ROOT/tools/verify_options_v15.kt" -include-runtime -d "$tmp/options.jar"
java -jar "$tmp/options.jar"
