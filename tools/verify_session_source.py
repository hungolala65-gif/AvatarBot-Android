#!/usr/bin/env python3
"""Static wiring guards; complements JVM policy unit tests (not an APK build)."""
from pathlib import Path
src=Path(__file__).resolve().parents[1]/'android/app/src/main/java/com/avatarbot/mobile'
service=(src/'service/BotForegroundService.kt').read_text()
ui=(src/'legacy/LegacyControlPanel.kt').read_text()
store=(src/'data/SecureConfigStore.kt').read_text()
assert 'StartAdmissionPolicy.acceptManualStart(' in service
# A duplicate Start must be stopped *before* it resets retry, worker account or network state.
start=service.index('ACTION_LEGACY_START, ACTION_LEGACY_RETRY -> {')
block=service[start:service.index('ACTION_START -> {',start)]
admit=block.index('StartAdmissionPolicy.acceptManualStart(')
reset=block.index('retry.reset()')
change=block.index('activeAccountId = id')
assert admit<reset and admit<change
assert 'if (LegacyBotRuntime.active() || observingLegacy) return START_NOT_STICKY' in service
assert 'val validCity =' in ui and 'account != null && validCity && !active' in ui
assert 'account != null && validCity && (mode != 0 || farmOptionsSupported)' in ui
assert 'object GameCities' not in store
main=(src/'MainActivity.kt').read_text()
assert 'else if (LegacyBotRuntime.active())' in main
print('PASS: Android Service duplicate-start guard is before session mutation; invalid city blocked in UI')
