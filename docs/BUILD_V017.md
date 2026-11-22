# Build a real v0.17 Android APK

## Option 1: Android Studio / local machine

1. Use JDK 17, Android SDK Platform 35, Build Tools 35.0.0, Android Gradle Plugin 8.7.3 and Gradle 8.9. Install Android SDK via Android Studio SDK Manager or Google's `sdkmanager` command-line tool.
2. Extract the whole ZIP to a directory. Set `ANDROID_HOME` to the Android SDK directory.
3. Run `bash tools/build_v017_apk.sh` from the root (or generate the wrapper with `cd android && gradle wrapper --gradle-version 8.9`, then run `./gradlew assembleDebug`).
4. A successful build creates `dist/AvatarBot-Android-v0.17-debug.apk` and its `.sha256`. Verify that checksum before distribution.

## Option 2: GitHub Actions

This archive contains `.github/workflows/build-apk.yml`. Upload **unpacked source tree** to a private repository that you own and are allowed to use. The workflow checks Java ME core, Kotlin source checks and Go tests, installs Android SDK 35, runs Gradle and uploads an APK artifact. Do not add game credentials, tokens, keystores, proxy passwords or private logs to GitHub.

The originally connected repository currently returns `403: Sorry. Your account was suspended`. This cannot be fixed by code changes. Follow GitHub's account reinstatement procedure (https://docs.github.com/en/site-policy/acceptable-use-policies/github-appeal-and-reinstatement) or use a different, authorized build environment.

## Signing and upgrades

Debug APKs built on different CI runners usually have different signing keys. Android may reject updating in place and uninstalling can delete Keystore-encrypted account data. For repeatable long-term updates, keep a release-signing keystore under your own secure control and configure signing secrets in a private build environment. Never commit a private key.

## Verification limits

Successful `assembleDebug` means the project compiled, NOT that login or any Auto succeeded with the live game server. Use a non-sensitive test account, verify the protocol and the device's Logs, then test each Auto independently. No automated check of real gameplay is included in this source bundle.
