# AvatarBot Android 0.10.0 — Reliability for user-initiated long-running sessions

## Changes implemented

- In per-account Auto settings, optional **Keep CPU awake** (Android PARTIAL_WAKE_LOCK) while the user-started foreground service runs. The lease is renewed every five minutes and released when the worker exits, the user stops it, or the service is destroyed. Off by default because it uses more battery.
- Optional recovery when a **previously started worker thread unexpectedly exits**. Conservative delays are 30, 60 and 120 seconds, maximum three attempts per explicit Start. No retry on initial startup/invalid credentials errors; pressing Stop cancels any pending retry. Off by default. It does not override JAR's own cycle reconnect logic or guarantee game authentication.
- Visible retry-wait state and uptime counter. Stop remains available while a retry is pending. Background notification explicitly says worker status does not prove a successful game login.
- Source tests for retry budget, stop/cancel behavior and all previous Java ME login/mock-server cases.

## Limitations (important)

- **No live-game confirmation**: LoginSuccess/Auto on a real game server has not been verified. JAR may require a different protocol version/endpoint/solver.
- **Single account**: JAR globals prevent safely hosting multiple independent logins in this process.
- **No automatic reboot start**: Android foreground restrictions and user-control requirements. Keep the foreground notification present while running.
- **Android battery optimizations may interrupt networking** even with a wake lock; users may choose battery settings manually. This release doesn't bypass vendor task-killers or Android restrictions.
- This is a **debug APK**, not a production-signed app. A new signing key can prevent install-over without uninstalling, which deletes local encrypted account storage.
- The Java ME JAR already repeats failed automation cycles internally, and these internal retries are not counted toward the 3 worker-restart limit. Avoid using important game accounts until logs confirm live login and execution.

## Offline checks

`bash tools/verify_offline.sh`: tests Go core, Kotlin importer/network/PSI syntax, JAR packet/handshake/login fixtures, lifecycle, proxy cancellation, configuration mapping and bounded retry policy. Android Gradle compilation must be performed by GitHub Actions / Android SDK.
