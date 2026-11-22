# AvatarBot Android v0.6 — CI + service life cycle

## Changes
- Fix Android Stop -> Start race: use `stopSelfResult(startId)` instead of a delayed unconditional `stopSelf()` after a queued Stop.
- Add a **Dừng bot** notification action which sends an explicit stop intent to the existing foreground service.
- Strengthen GitHub Actions: run Java ME and Go offline tests on Ubuntu before Android build (Android Kotlin checked by Gradle), assert APK existence, upload SHA-256 alongside the APK, keep failure build diagnostics.
- Version bump to 0.6.0.

## Important limitations
Android APK is **not** produced or tested in this offline environment; CI has **not** been invoked because no writable repository was visible for the linked GitHub account. Real game server login and each Auto module remain unverified.
