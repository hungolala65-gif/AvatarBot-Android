# AvatarBot Android — development toward release

**Latest validated Android build:** v0.18.0, **debug/testing only**.

- [Complete, inspectable Kotlin / Java / Go sources on the `source-v0.18` branch](https://github.com/hungolala65-gif/AvatarBot-Android/tree/source-v0.18)
- [v0.18 Android build + unit tests + APK artifact](https://github.com/hungolala65-gif/AvatarBot-Android/actions/runs/44968251636)
- [v0.18 release gates and open limitations](https://github.com/hungolala65-gif/AvatarBot-Android/blob/source-v0.18/docs/RELEASE_GATES_V018.md)
- [v0.18 changes](https://github.com/hungolala65-gif/AvatarBot-Android/blob/source-v0.18/docs/CHANGELOG_V018.md)

## Reproducible debug build

1. The workflow checks out this repository and extracts the original v0.17 source ZIP.
2. It applies the tracked patch at `patches/v0.18.patch.zlib.b64`.
3. It runs Java ME offline tests, per-account setup persistence tests, Go tests, Android Gradle unit tests and `assembleDebug`.
4. It checks the APK ZIP, verifies its debug signature, and uploads **APK + fully patched source** as an Actions artifact.

Latest validated APK SHA-256:
`33054226cdab8efd016efec50576119313571ac306c74bcb6bac8c4301754e23`

The user-supplied Windows/Linux AvatarBot PC archive contains **compiled binaries, not Go source code**; the independent Android application uses Java ME JAR logic and Android adapters. It is **not feature-equivalent to the PC**. Not yet validated: live-game actions, concurrent accounts, 8–24 hour unattended execution, release keystore signing and store policy. Worker running or LoginSuccess does **not** imply completed gameplay tasks.

**Privacy:** This repository is public. Do not commit game credentials, account exports, private logs or keystore credentials. Redistribution rights for supplied binaries/JAR must be reviewed before distribution.
