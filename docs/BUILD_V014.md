# Build v0.14 — source first

The root of this ZIP is the project root. Extract **all contents** into a private Git repository. The former public repository still contained a v0.6 ZIP and CI patches; do not merely upload this v0.14 ZIP expecting old CI to build the new version. Replace root files and keep the included `.github/workflows/build-apk.yml` workflow.

## Local offline verification

From the root: `bash tools/verify_offline.sh` (requires Java 17, Go, Kotlin CLI, Python 3). No live game server is contacted.

## APK build

Install Android SDK Platform 35, Build Tools 35.0.0, JDK 17, and Gradle 8.9. From the `android/` directory run:

```sh
gradle wrapper --gradle-version 8.9
./gradlew --no-daemon assembleDebug
```

On a repository with GitHub Actions available, the included workflow builds the ZIP's **extracted sources** and archives `app-debug.apk` together with its SHA-256 file. A successful offline test alone does not prove the APK builds, installs or logs in.

Do not upload passwords, proxy credentials, game-account vaults, or personal logs to a public repository. Check your right to redistribute the supplied JAR before publishing the repository or APK.
