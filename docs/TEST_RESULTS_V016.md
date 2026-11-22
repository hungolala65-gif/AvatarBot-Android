# AvatarBot v0.16 — Test results and delivery status

## Actual local test results

- `bash tools/verify_legacy.sh`: PASS (all tests; includes original JAR handshake/login, RMS, SOCKS5, Start/Stop, four Auto modes, setup mapping, new live-session inspector and mock-server integration).
- `LiveSessionIntegrationOfflineTest`: PASS — original JAR LoginSuccess on mock TCP → `confirmed` → `disconnected` after Stop.
- `LiveSessionInspectorOfflineTest`: PASS — unauthenticated, no session, ID after login and closed session.
- `bash tools/verify_setup_options_v15.sh`: PASS — account-specific configuration, range checks, JSON round trip.
- `go test -race ./...`: PASS.
- `go vet ./...`: PASS.
- `CGO_ENABLED=0 GOOS=android GOARCH=arm64 go build ./...`: PASS (only experimental Go utilities; **not** an Android AAR).
- Kotlin PSI parser: PASS — 11 Kotlin source files checked syntactically; not equivalent to a Gradle semantic build.
- The monolithic `tools/verify_offline.sh` was interrupted by a sandbox time limit **after** Go/Kotlin parser and network checks had printed PASS. Remaining JAR and setup tests were run separately and passed.

## Build status

- No Android SDK, Gradle, or Android dependency repository network access in the active build container.
- Connected GitHub currently responds `HTTP 403: Sorry. Your account was suspended`, so CI could not be triggered for v0.16.
- **No v0.16 APK was generated**, and no installation/device/in-game evidence exists for this version.
- Build workflow `.github/workflows/build-apk.yml` is provided for a functioning Android CI runner with source tree.

## Verify a generated APK on an available runner

Run `bash tools/verify_legacy.sh` and `bash tools/verify_offline.sh`; then from `android/` use Android Studio / Gradle to run `assembleDebug`. Verify `android/app/build/outputs/apk/debug/app-debug.apk` exists and compare SHA-256, install on a disposable/test Android device, and first test with a test account. Do not call the result production-ready until game login and Auto actions succeed and long-duration behavior is measured.
