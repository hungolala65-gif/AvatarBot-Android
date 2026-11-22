# AvatarBot Android Native v0.14 — Harden session starts and game destination handling

## Actual fixes (checked in source)

1. **Duplicate Start race in Android Service:** `BotForegroundService.onStartCommand` now checks `StartAdmissionPolicy` before resetting retry budget, cancelling pending retry, changing `activeAccountId` or `activeMode`. If one session is running, observing, scheduled to retry or waiting for connectivity, an unsolicited second Start is ignored. The user must Stop first before switching accounts/modes.
2. **Old Go action cannot terminate the JAR worker:** An obsolete `ACTION_START` command now ignores itself while the JAR worker or its monitor is active. Unknown/invalid commands only call `stopSelfResult` when no JAR session/monitor is active.
3. **Fail-closed game server selection:** Removed the implicit fallback from arbitrary city codes to Hoàn Mỹ. `GameCities.endpoint` now accepts only `hoanmy` or `dieuky`; any unknown city code throws before transmitting credentials. Invalid saved cities are displayed safely and the Start/Login Test buttons are disabled.
4. **Login-only UI safety:** The login-test coroutine catches config validation errors and surfaces a class-only diagnostic instead of leaving an unhandled coroutine exception. The Start button now reports an already active session rather than falsely saying a new bot run was requested.
5. **Regression checks:** Added a Java policy test covering 16 combinations, a Kotlin strict-server test covering 5 invalid city codes, and a source-wiring assertion ensuring the admission check happens before session mutation.

## Not addressed by v0.14

- No verified LoginSuccess from real Avatar 2X server, no real Auto Farm/Fishing/Teacher/Diamond testing, no Android device stability/battery testing.
- User-provided PC archive holds only Windows/Linux compiled Go binaries, no Go sources. JAR uses global static state, supporting only one concurrent account in this Android port.
- No claim of 24/7 background reliability. The Android foreground service and CPU WakeLock remain best-effort.
- No APK v0.14 built locally: Android SDK/Gradle are absent and the configured GitHub account currently returns HTTP 403 (suspended). This version is source only.

## Validation

`bash tools/verify_offline.sh`: Go/race/vet, Go Android ARM64 compile check, Kotlin JVM/PSI, Java ME and offline mock TCP/SOCKS5. Note Kotlin PSI does not prove Android Gradle compilation.
