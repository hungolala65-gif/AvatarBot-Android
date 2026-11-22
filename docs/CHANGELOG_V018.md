# AvatarBot Android v0.18 — Farm parity hardening

Compared against user-provided `AvatarBot-Fix-Full.zip` (Go executables + user guide, no Go source) and `Bot2X-SuperLite.jar` (Java ME bytecode).

## Implemented

- Farm inventory sale **opt-in**, explicit positive product ID whitelist (1–16 entries), and minimum stock threshold. No `-1` wildcard or sell-everything mode.
- Farm optional `rebuy_sold` logic for animals the JAR sold. Default off. JAR `noLuongBuy=true` remains enforced.
- Farm optional star-tree upgrade and configurable target. Default off; may spend in-game currency when enabled.
- Settings saved per Android account in encrypted vault and mapped into existing JAR `Config` fields only in Farm mode.
- Added regression tests for range validation, default no-sale/no-rebuy/no-upgrade and mode isolation.
- Version code 18, version 0.18.0.

## Missing

The PC Go executable is not source code. There is no safe way to claim 1:1 port; the JAR lacks independent controls for all crop/animal tasks in the PC. Android remains a single-worker app. End-to-end game-server login, gameplay actions, long-run device reliability and release signing are not validated. Debug APK is for controlled tests only.
