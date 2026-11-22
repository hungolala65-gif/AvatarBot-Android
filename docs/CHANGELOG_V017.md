# AvatarBot Android v0.17 — change log

### Genuine source improvements

1. `LegacySessionInspector.authenticatedCycleIdentity`: read-only detection of a new `farm2x.Bot` that received a positive ID from `LoginSuccess` in the supplied JAR. The evidence remains inspectable after its socket closes and is reset when a new Bot cycle starts. It never counts an Auto task as successful.
2. `LegacyBotRuntime.observedLoginCycles()`: count a unique authenticated JAR Bot object only once per worker run, with account separation through the single-worker architecture.
3. `AuthWatchdogPolicy`: finite, conservative initial-authentication guard (3 min) and reconnect grace (10 min), using Android's monotonic elapsed clock. Stops if valid read-only observations show an unauthenticated worker for too long, rather than indefinitely showing a misleading running state. Introspection failure is treated as unknown, not proof of failure.
4. Android Foreground Service: reads login evidence, preserves encrypted event trail (`LIVE_LOGIN_CONFIRMED`, `LOGIN_RECONFIRMED`, `AUTH_STALLED`), cancels pending retry and releases locks when stalled; does not auto-retry rejected credentials.
5. Android UI: indicates number of JAR cycles with observed LoginSuccess, explicitly **not** number of Auto actions; displays health-policy explanation.
6. `tools/legacy-tests/AuthWatchdogPolicyOfflineTest.java` and `LoginCycleEvidenceOfflineTest.java`: test timers, logout/reconnect, cancellation reset, missing reflection, rapid closed-session evidence, identity separation.
7. Version bump to 0.17.0; added `tools/build_v017_apk.sh` and updated CI artifact naming.

### Current hard limitations

- No real-server test credentials/results or live game observations: cannot attest that Farm, Fishing, Teacher or Diamond completed anything in game.
- JAR contains global static stores; running more than one account simultaneously is not safe without isolating every Java ME static/session state.
- Original PC ZIP includes only compiled Windows/Linux binaries and documentation, no Go source to transplant.
- Android Gradle build blocked in this environment; GitHub API authentication currently rejected with HTTP 403 account suspended.
