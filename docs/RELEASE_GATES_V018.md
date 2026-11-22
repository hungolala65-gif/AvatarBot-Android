# Release gate v0.18 (NOT APPROVED)

## Required before official release

1. Verify allowed redistribution and integration rights for PC binaries and Java ME code.
2. Test `LoginSuccess` and in-game acknowledgements on both cities with designated test accounts, never using test credentials in GitHub logs.
3. Establish verifiable task-level acknowledgements for farming, fishing, teacher and diamond modes; do not equate a live worker or `LoginSuccess` with successful tasks.
4. Implement secure per-account isolation for parallel execution or publicly document single-account limitation.
5. Run Android device matrix tests for Android 8–16 including notification permission, Doze, OEM power restrictions, background stop and reconnect across 8–24 hours.
6. Complete data security/privacy review, explicit account credential deletion/export policy, sensitive logs and proxy handling.
7. Implement stable release keystore storage, signing, Play foreground-service special-use declaration review, crash reporting opt-in and reproducible release build. Never share a keystore or signing passwords.
8. Automate instrumented UI tests, integration tests against a mock protocol and release checklist signoff.

Debug build passing CI is **not** official release readiness.
