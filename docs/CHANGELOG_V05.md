# AvatarBot Android Native v0.5 — 20/11/2026

## Các thay đổi kiểm chứng bằng mã

- Thêm mã vé khởi chạy (`launchGeneration`) trong `LegacyBotRuntime`: nếu người dùng bấm Dừng khi tác vụ còn đọc kho mật khẩu, tác vụ cũ không thể khởi chạy bot sau đó.
- Lỗi bất đồng bộ từ tác vụ cũ không thể ghi đè trạng thái worker mới. Lỗi khởi chạy hiện tại chỉ hiện tên loại ngoại lệ, không chứa mật khẩu hay địa chỉ proxy.
- `BotForegroundService` kiểm tra vé trước và sau khi nạp dữ liệu; sử dụng `stopSelfResult(startId)` để tránh dừng nhầm yêu cầu Service mới.
- Đặt timeout 10 giây khi thương lượng SOCKS5 sau khi TCP đã kết nối; lúc Dừng, mọi socket đang mở bị đóng ngay.
- Thêm `StartTicketOfflineTest.java` và `SocksHandshakeCancellationTest.java` vào chuỗi kiểm thử JAR.
- Nâng `versionCode` lên 5, `versionName` thành `0.5.0`.
- GitHub Actions có thêm artifact nhật ký lỗi build nếu bước Gradle thất bại.

## Phạm vi đã xác minh

- 50/50 lớp bytecode trong `bot2x-headless.jar` trùng chính xác với JAR gốc.
- Java ME bridge và các bài kiểm thử kết nối/mock login dùng JVM chạy offline, không truy cập server game.
- Đã kiểm tra cú pháp Kotlin; chưa có Android SDK/Gradle để xác nhận DEX/Compose build.

## Vẫn chưa thể xác nhận

- Build APK trên thiết bị/CI, chạy Android Foreground Service trên thiết bị thật.
- Đăng nhập thành công và các lệnh Auto trên game server thật.
- Hoạt động 24/7 trên mọi phiên bản Android hoặc đồng thời nhiều tài khoản.
