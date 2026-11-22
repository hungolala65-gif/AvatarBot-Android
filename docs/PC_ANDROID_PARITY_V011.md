# AvatarBot Android 0.11 — PC comparison / technical readiness

Sources actually inspected: `AvatarBot-Fix-Full.zip` (PC guide, PE/ELF Go binaries, extracted Go metadata) and `Bot2X-SuperLite.jar` (Java ME bytecode), plus Android 0.10 sources. The PC ZIP contains **no Go source code**. The JAR is a **different implementation**, not proof that Android exactly clones the PC tool.

| PC capability | Android 0.11 status | Evidence / caveat |
|---|---|---|
| Windows PC executable / web UI | Android Compose UI | Different interface, not a 1:1 clone |
| Farm automation | JAR farm worker integrated | Server not verified, advanced farm options not mapped |
| Fishing/teacher/diamond | JAR workers integrated | Offline mock tests, not live game |
| Many accounts in parallel | **Not supported** | Global JAR state; only one worker allowed |
| PC proxy/VPS-node manager | SOCKS5 per account; no VPS node manager | Different feature set |
| Account credential vault | Android Keystore AES-GCM | Cannot import PC vault.dat |
| PC configurable crop/animal/seed policies | Partly missing | Android does not map all JAR and Go options |
| Login | JAR connectLogin probe | Only mock LoginSuccess/LoginFail tested |
| Long-running background service | Android Foreground Service, optional WakeLock | No 24/7 guarantee; device test absent |
| Auto cycle cadence | **Added** selectable 15/30/60/120/300 s | Mapped to `Config.loopDelayMs`; JAR worker reads that field |

## Release gates
1. Can Gradle compile and sign a debug APK? Check CI artifact and checksum.
2. Can a user install and open it on actual Android devices? Unverified until user tests.
3. Can a test account receive `LoginSuccess` from the live game server? Unverified.
4. Can Farm/Fishing/Teacher/Diamond execute and stop cleanly on live server? Unverified.
5. Does one account remain stable for 8–24 hours with screen off and changing networks? Unverified.
6. Are legal rights to redistribute the PC/JAR binaries and any server restrictions understood? Unverified.
7. Is a stable production release key available and has Play Store foreground service policy been reviewed? Not completed.

No percentages in this document are interpreted as measured test coverage. An overall production-readiness number is necessarily a judgement until the above tests are performed.
