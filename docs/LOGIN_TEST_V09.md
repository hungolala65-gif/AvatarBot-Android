# AvatarBot Android v0.9 — Login-only authentication diagnostics

## What is verified

The JAR's actual `farm2x.Bot.connectLogin(false)` sends a Java ME KeyRequest/handshake, the four initialization packets, and the Login packet. It waits for game responses. The Android app now exposes a **Kiểm tra đăng nhập** button near the top of its Dashboard and on the Auto tab. The test does **not** start Farm/Fishing/Diamond/Teacher automation.

- `success` means the JAR received **LoginSuccess** and processed it. This does not imply Auto commands have been tested.
- `rejected` means no LoginSuccess was returned (including a LoginFail reply, an incomplete/closed connection, or the 15s login timeout). Check masked Logs.
- `error` means a Java/network/protocol exception occurred.
- `cancelled` means Stop interrupted the operation.

A separate mock-server regression test covers both `LoginSuccess` and `LoginFail`, checks no worker is left behind and checks that the displayed result does not expose the password. Tests can be run with `bash tools/verify_offline.sh`.

## Test on your device

1. Install the debug APK. If updating from an APK signed by a different debug key, save your account details before reinstalling: uninstalling may erase the encrypted local account store.
2. In **Tài khoản**, enter the username, password, and the correct **Hoàn Mỹ** or **Diệu Kỳ** city. Credentials remain encrypted on the device.
3. Open **Tổng quan**, select the account, tap **Kiểm tra đăng nhập**. Watch the result and **Logs**.
4. Do not start Auto while testing. A successful Login check proves only authentication, not automation.
5. If it fails, share the result and masked Logs, NEVER the password or raw credentials.

## Known limitations

- The JAR contains default client version `2.5.8`, `clientType=8`, `provider=0`, `versionCode=13`, and `deviceID=j2me-bot`. These come from JAR bytecode and may no longer match a live server.
- Default hosts are `112.213.85.102:19128` (Hoàn Mỹ) and `112.213.85.101:19128` (Diệu Kỳ). They are taken from the supplied archive/JAR, not live server verification.
- No personal game credentials or successful real-server logs were provided. The app has **not** been validated on a live server.
- Only one JAR session runs at once because its static state is shared.
- The external CAPTCHA solver endpoint remains blocked. Features requiring it may be unavailable.
- The PC Go backend is not included as source, so full PC feature parity is not verified.
