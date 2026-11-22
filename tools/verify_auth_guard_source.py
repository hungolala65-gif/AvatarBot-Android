#!/usr/bin/env python3
"""Check the Android wiring for the tested pure-Java login health guard.
Not a Kotlin/Android semantic compiler: does not replace Gradle assembleDebug.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
service = (root/'android/app/src/main/java/com/avatarbot/mobile/service/BotForegroundService.kt').read_text()
journal = (root/'android/app/src/main/java/com/avatarbot/mobile/data/SecureExecutionJournal.kt').read_text()
panel = (root/'android/app/src/main/java/com/avatarbot/mobile/legacy/LegacyControlPanel.kt').read_text()
checks = {
 'Java guard instantiated': 'private val authWatchdog = AuthWatchdogPolicy()' in service,
 'guard reset for each start': 'authWatchdog.reset()' in service,
 'guard samples actual session': 'authWatchdog.observe(' in service and 'if (newLoginEvidence) "confirmed" else auth' in service,
 'watchdog alarm logged': 'event(ExecutionStage.AUTH_STALLED)' in service and 'AUTH_STALLED(' in journal,
 'login reconfirmation logged': 'LOGIN_RECONFIRMED' in service and 'LOGIN_RECONFIRMED(' in journal,
 'auth evidence not lost after socket close': 'observedLoginCycles()' in service,
 'watchdog cancels retry': 'retry.cancel()' in service,
 'watchdog closes worker': 'LegacyBotRuntime.stop()' in service,
 'worker still distinguished from auth': 'liveLoginState()' in panel,
 'Android version set correctly': 'versionCode = 18' in (root/'android/app/build.gradle.kts').read_text(),
}
for label, ok in checks.items():
 print(('PASS' if ok else 'FAIL') + ': ' + label)
if not all(checks.values()): raise SystemExit(1)
