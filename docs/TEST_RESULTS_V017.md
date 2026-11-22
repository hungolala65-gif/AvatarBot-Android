# AvatarBot Android v0.17 — result of this session

## Verified from the exact user-provided files

- PC file `AvatarBot-Fix-Full.zip`: 3 entries, including guide and Windows/Linux amd64 binaries; 0 `.go` source files.
- `Bot2X-SuperLite.jar`: 60 original `farm2x` class files; 50 are embedded in the Android headless JAR and all 50 matched original bytecode exactly.
- `farm2x.BotWorker.run()` constructs a fresh `Bot` for each cycle and catches cycle-level exceptions, so a live worker thread alone does not prove login or task completion.
- JAR `Bot.connectLogin` assigns positive `ownerID` upon LoginSuccess (-4). The new v0.17 monitor captures these positive IDs even when the connection closes between 1.5-second polls.

## Checks executed and passed

1. `bash tools/verify_legacy.sh` — success; includes original JAR handshake/LoginSuccess/LoginFail mock-server tests, core options/setup, SOCKS5 cancellation, new AuthWatchdogPolicy test and LoginCycleEvidence test.
2. `bash tools/verify_setup_options_v15.sh` — success; encrypted per-account config options, supported range, copy semantics, legacy flags.
3. `python3 tools/verify_session_source.py` — success; Android Service guards against stale starts and invalid city.
4. `python3 tools/verify_auth_guard_source.py` — success; source-level check that Android Service resets/uses auth health and reports events.
5. `go test -race ./...`, `go vet ./...`, `CGO_ENABLED=0 GOOS=android GOARCH=arm64 go build ./...` inside `go-mobilecore` — passed.
6. Kotlin JVM importer tests (8 checks), GameCities tests (5 invalid cities), DiagnosticRedactor tests, Kotlin TCP/SOCKS5 fixture tests — passed.
7. Kotlin PSI syntax parse — passed for 11 Kotlin app source files. Syntax-only; NOT an Android Gradle semantic build.
8. Original vs Android `farm2x` classes — verified byte-for-byte (50/50 matched).

## Attempted, blocked, not passed

- `bash tools/build_v017_apk.sh` exits **20**: Android SDK 35 not available; `ANDROID_HOME` unset. No new APK was produced.
- `gradle` command and Android build tools are absent. Network lookup for official Android and Gradle package hosts failed, so SDK installation could not proceed here.
- GitHub connected repository `hungrose15620-wq/AvatarBot-Android` returns **403** with message `Sorry. Your account was suspended`. No source changes were pushed or Actions build triggered in this session.
- Real-server game login and Auto cycles: **not tested**. Only mock TCP fixtures, no live player credentials. Do not claim 24/7 operation.

## Known caution

- This JAR uses global static state, so the Android app intentionally limits execution to one worker/account at once. Multiple simultaneous accounts would require isolation, not enabling a hidden flag.
- The original PC version uses a different Go backend; its binary cannot be converted to ARM64 Android without original Go sources or a full verified reimplementation.
- The v0.17 authentication guard stops a worker after 3 minutes without verified initial LoginSuccess or 10 minutes without observing a renewed LoginSuccess after prior verification. Reflection failures return UNKNOWN rather than terminating on unverified assumptions. These guard timings are engineering policy, not proof of game server behavior.
- Reusing a debug APK signed with a different key can require reinstalling; reinstalling deletes app-owned encrypted data.
