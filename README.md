# AvatarBot Android v0.17 — APK debug build

**Build status:** GitHub Actions run [#2](https://github.com/hungolala65-gif/AvatarBot-Android/actions/runs/44916495354) completed successfully on 2026-11-21.

## Source

- Source ZIP: `AvatarBot_Android_Native_v0.17_AuthGuard_Source.zip`
- SHA-256 of built APK (`app-debug.apk`): `474afddff74d9d8ff3e86aa1160164dd58e77816fde087e4764123f2d989ae18`
- Minimum Android version: Android 8.0 / API 26 (`minSdk = 26`)
- Application ID: `com.avatarbot.mobile`
- Version: `0.17.0`

## Download and install

1. Open the successful [GitHub Actions build](https://github.com/hungolala65-gif/AvatarBot-Android/actions/runs/44916495354).
2. Under **Artifacts**, download `avatarbot-v017-debug-apk` (requires GitHub sign-in; retained for 14 days).
3. Extract the artifact ZIP and install `app-debug.apk` on an Android device.
4. Verify the file's SHA-256 hash if possible.

## What was verified

- The source ZIP extracted successfully.
- Offline Java ME / Go test suites completed.
- Android SDK 35 / Gradle compiled `assembleDebug` successfully.
- The artifact was uploaded successfully; the resulting APK passed ZIP integrity and checksum checks.

**Not yet verified:** successful login to the live game server, real Farm/Fishing/Teacher/Diamond game actions, multi-account operation, or extended 24-hour runtime. This is a **debug/test build**, not a release build.

## Privacy and safety

The repository is currently **public**. Do not commit any real game account credentials, logs, API keys, or other personal data. Review ownership/redistribution rights for the included legacy Java ME library.

## Rebuild

Replace the source ZIP at the repository root with a tested updated archive to trigger the workflow; or use **Actions → Build AvatarBot Android v0.17 from ZIP → Run workflow**.
