# Phân tích Bot2X-SuperLite.jar và chuyển sang Android — v0.3

## Những điều xác nhận trực tiếp bằng bytecode

Nguồn `Bot2X-SuperLite.jar` người dùng cung cấp: 132.454 byte, SHA-256:
`089b59d59945f9d3e05c3608c170e9e8fbfe5d093f04aa80a9c632e77d4d1d00`.

- 60 tệp `.class`, package `farm2x`, Java class file major 52 (Java 8), MIDlet J2ME theo manifest (`MIDP-2.0`, `CLDC-1.1`). **Không chứa file Java source.**
- `Session.connect`: `socket://host:port`, gửi `Cmd.KeyRequest=-27`, nhận packet không mã hóa, lấy độ dài khóa `u8`, giải mã XOR prefix, lập read/write cipher streams riêng, sau đó đọc/ghi khung `command(1) + length(2, big endian) + payload`. XOR được áp dụng lên cả header và payload sau bắt tay.
- `Bot.bootstrap`: gửi `SetHandler=-1`, `SetProvider=-17`, `SetAgent=-79`, `Device=-86`.
- `Bot.connectLogin`: gửi `Login=-2`, đợi `LoginSuccess=-4` hoặc `LoginFail=-5`.
- `BotWorker` có 4 mode chính (Farm=0, Fish=1, Diamond=2, Teacher=3). `Bot` có `runFarmCycle`, `runFishingCycle`, `runTeacher`, `runDiamondCycle`, hàng chục tác vụ bổ trợ.
- `Payloads` tạo packet nông trại / câu cá / casino / NPC; `Parsers` giải mã farm state, inventory, catalog, NPC, chat; `Diamond` tính nước đi.
- `Config` dùng Java ME RMS để lưu cấu hình; `Stats` và `TeacherStore` cũng dùng RMS.
- `Session` và `SolverClient$Job` truy cập mạng thông qua `javax.microedition.io.Connector`. `SolverClient` có endpoint giải cá/CAPTCHA bên ngoài. `SolverSecret` chứa bí mật nhúng trong nhị phân. Không gửi bí mật này hoặc lưu trong log.

## Những gì đã triển khai thực sự

1. Tạo `android/app/libs/bot2x-headless.jar` từ **50 lớp core của JAR thật**, bỏ 10 lớp giao diện MIDlet/Canvas không phù hợp Android. Toàn bộ tham chiếu `farm2x/*` còn đầy đủ.
2. Bổ sung Java ME compatibility: `javax.microedition.io.{Connection,SocketConnection,Connector}` và `javax.microedition.rms.*`. `Connector` chỉ cho phép TCP đến endpoint game đã chọn, hoặc đi qua SOCKS5 xác thực (endpoint đích vẫn được kiểm tra). Tất cả endpoint ngoài danh sách cho phép đều bị từ chối, bao gồm dịch vụ solver. Đây không phải mô phỏng bot; core JAR gốc được thực thi.
3. RMS dùng bộ nhớ RAM, không ghi mật khẩu hoặc câu trả lời Teacher ra tệp không mã hóa. **Dữ liệu Teacher/RMS hiện không bền vững khi ứng dụng bị đóng hoàn toàn.** Profile Android riêng vẫn dùng AES-GCM + Keystore.
4. `LegacyBotRuntime.java` chạy `farm2x.BotWorker` thật, cho phép 1 tài khoản/mode mỗi lần (JAR có biến toàn cục Log, Wallet, Stats, TeacherStore). Có `start`, `stop`, `running`, `logs`.
5. `BotForegroundService.kt` đọc tài khoản đã mã hóa bằng ID (không truyền mật khẩu qua Intent), cho phép người dùng chọn chế độ rồi chạy/dừng worker trong foreground. UI tab Auto bổ sung `LegacyControlPanel.kt`.
6. GitHub Actions tạo debug APK từ Java ME core đã nhúng mà **không đòi Go AAR, NDK hay gomobile**. Go core v0.2 vẫn giữ nguyên trong repo cho nghiên cứu.

## Kết quả test offline

`tools/verify_legacy.sh`: PASS.

- 44 khẳng định về JAR Config, opcode, packet codec, Unicode, kết nối trực tiếp, SOCKS5 không xác thực và SOCKS5 username/password, chặn endpoint khác, key handshake, mã hóa packet vào/ra; test bằng loopback mock TCP server, **không giao tiếp server game thật**.
- `LoginFlowOfflineTest`: **PASS** cho giao thức bootstrap thật (4 opcode), packet `Login` thật được tạo bằng `Payloads.login`, phản hồi login-fail giả lập được xử lý đúng bởi `Bot.connectLogin` (phương thức riêng được gọi trong JVM fixture). Không có tài khoản thật.
- `go test -race ./...`, `go vet ./...`, `GOOS=android GOARCH=arm64 CGO_ENABLED=0 go build ./...` cho Go Core phụ trợ: PASS.
- Kotlin importer và probe: PASS. Kotlin PSI syntax: PASS. Chưa kiểm thử semantic Android Gradle hoặc chạy trên Android thật.

## Giới hạn và an toàn

- **APK chưa được biên dịch trong môi trường hiện tại** do thiếu Android SDK/Gradle. Android Studio hoặc GitHub Actions đã cấu hình có thể thử build, nhưng chưa xác nhận CI chạy thành công.
- Chưa kiểm thử login **thành công** và 4 Auto mode trên server game thật. Server có thể khác phiên bản hoặc không chấp nhận gói từ JAR cũ; chưa có bằng chứng tương thích.
- `SocketConnection` sử dụng TCP + XOR truyền thống của trò chơi, không phải TLS; không nên coi là mã hóa bảo mật mạnh đối với mật khẩu.
- JAR chưa được audit hoàn toàn mọi nhánh. Nền tảng network allowlist hạn chế việc liên hệ dịch vụ solver bên thứ ba; các chức năng dựa vào solver có thể thất bại hoặc cần thay bằng cơ chế giải thủ công/được phép.
- Chỉ 4 chế độ và 2 tùy chọn (lái buôn, nấu ăn) đã được ánh xạ từ Android UI vào `Config`. Các công tắc UI còn lại (thu hoạch/gieo/tưới/chăm vật nuôi/thu khế) mới lưu trong Keystore, không trực tiếp điều khiển JAR. JAR tự dùng lựa chọn mặc định.
- JAR có trạng thái tĩnh toàn cục, chỉ chạy 1 tài khoản đồng thời; cần làm sạch/đóng gói nhiều classloader hoặc sửa source nếu cần chạy nhiều tài khoản.
- Foreground Service chịu giới hạn Android và Google Play; không bảo đảm hoạt động 24/7.
- Không thể bảo đảm tránh khóa tài khoản / tuân thủ điều khoản game. Thử nghiệm cẩn trọng theo quyền sử dụng của người dùng.

## Build Android APK khi có toolchain

```bash
bash tools/verify_legacy.sh
bash tools/build_android.sh
```

Yêu cầu JDK17, Android SDK 35, Gradle 8.9 và môi trường tải dependency Compose. APK sẽ được tạo tại `android/app/build/outputs/apk/debug/app-debug.apk` nếu compile thành công. Repo có `.github/workflows/build-android.yml` để build qua GitHub Actions.

## Các việc ưu tiên tiếp theo

1. Chạy CI Android và sửa compile/manifest errors cụ thể nếu xuất hiện.
2. Cài APK trên máy Android thật, kiểm tra Keystore, permission và foreground service.
3. Kiểm thử login / packet thật chỉ với máy chủ được phép và tài khoản thử; không thu thập mật khẩu.
4. Rà soát, thay hoặc tắt phụ thuộc solver ngoài; bảo đảm quyền riêng tư.
5. Ánh xạ các tùy chọn `Config` nâng cao còn lại; kiểm thử các hành động tương ứng.
6. Thiết kế tách state để hỗ trợ đồng thời nhiều tài khoản (không bật ồ ạt JAR static code hiện tại).
