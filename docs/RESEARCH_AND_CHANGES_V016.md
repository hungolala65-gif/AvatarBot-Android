# AvatarBot Android v0.16 — Kiểm tra JAR, PC và tích hợp trạng thái đăng nhập

## Nguồn đã kiểm tra
- `AvatarBot-Fix-Full.zip`: chỉ có hướng dẫn và hai nhị phân Windows/Linux amd64; không có mã nguồn Go.
- `Bot2X-SuperLite.jar`: có `farm2x.BotWorker`, `Bot`, `Session`, `Config`, `Cmd`, `Parsers` và logic Java ME cho Auto Farm/Fishing/Diamond/Teacher.
- Bộ nguồn Android v0.15: dùng Java ME headless, chỉ hỗ trợ **một worker** tại mỗi thời điểm, lưu tài khoản mã hóa bằng Android Keystore.

## Phát hiện từ bytecode gốc
- `BotWorker.run()` bắt `Throwable` trong vòng lặp và tiếp tục sau `loopDelayMs`. Vì vậy một luồng còn sống **không phải** bằng chứng đăng nhập thành công.
- `Bot.connectLogin(boolean)` chỉ gán `ownerID` khi nhận opcode `LoginSuccess (-4)`; trên `LoginFail (-5)` trả về false.
- JAR giữ `BotWorker.bot` và `Bot.session` bên trong đối tượng, `Session.isClosed()` báo trạng thái phiên hiện tại.
- Bản PC hỗ trợ nhiều tài khoản và có các tác vụ nông trại độc lập. Đây vẫn là khác biệt quan trọng với Android.

## Các thay đổi v0.16
1. Bổ sung `LegacySessionInspector` (read-only) đọc bot hiện tại, `ownerID` và tình trạng socket trong JAR qua reflection.
2. Không sử dụng `thread.isAlive()` để suy ra đăng nhập thành công nữa: `liveLoginState()` chỉ trả `confirmed` khi thấy ID nhân vật dương trên phiên chưa đóng. `awaiting`, `disconnected`, `unavailable`, `inactive` được phân biệt.
3. Tab Setup và Tổng quan hiển thị riêng **trạng thái worker** với **trạng thái xác thực** khi tài khoản được chọn là tài khoản đang chạy.
4. Android Foreground Service cập nhật notification khi trạng thái phiên thay đổi, và lưu một sự kiện `LIVE_LOGIN_CONFIRMED` mã hóa theo tài khoản cho mỗi lượt Start khi quan sát được LoginSuccess.
5. Không chỉnh sửa packet JAR hoặc kích hoạt các chế độ bán vật phẩm ngoài lựa chọn người dùng; không cung cấp thống kê Auto thành công giả.

## Kiểm thử offline
- Bộ `LiveSessionInspectorOfflineTest`: thử worker chưa có Bot, Bot chưa có socket, socket chưa đăng nhập, có mã nhân vật sau LoginSuccess và socket đóng.
- Bộ `LiveSessionIntegrationOfflineTest`: chạy **JAR thật** với TCP server giả lập gửi handshake và `LoginSuccess`, xác nhận monitor chuyển `confirmed` rồi `disconnected` sau Stop.
- Bộ `tools/verify_legacy.sh`: kiểm thử JAR, giao thức, nhiều chế độ, Start/Stop, SOCKS5, LoginSuccess/Fail và setup theo tài khoản.
- Bộ `go test -race ./...`, `go vet ./...`, build Go ARM64 đều đạt.
- Kotlin JVM tests và Kotlin PSI đã chạy; không thay cho Gradle Android semantic build và thử thiết bị.

## Cần thử nghiệm tiếp
- **Đăng nhập trên server Avatar thật**: chưa có phản hồi thực tế từ điện thoại để xác nhận client version, handler, provider, session key và trạng thái game.
- **Kết quả thao tác Auto**: xác nhận bằng gói trả về game, không căn cứ vào độ sống luồng hoặc log không có đối chiếu.
- **Nhiều tài khoản**: Java ME dùng trạng thái toàn cục, chưa thể chạy an toàn đồng thời.
- **Treo dài hạn**: cần thử trên thiết bị Android nhiều giờ và theo dõi mất mạng / khóa nền / tiêu thụ pin.
- **APK v0.16**: chưa build được trong phiên này vì không có Android SDK/Gradle, GitHub connector trả 403 `Your account was suspended`. Workflow `.github/workflows/build-apk.yml` có sẵn, sẽ build APK khi chạy trên runner hợp lệ.

Không đưa mật khẩu thật, token, tài khoản thật hoặc log có dữ liệu nhạy cảm lên GitHub công khai. Nếu tạo repository mới để build, đặt ở chế độ Private và không vô hiệu hóa bảo vệ bí mật.
