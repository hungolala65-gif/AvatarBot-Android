# Những thay đổi của AvatarBot Android Native v0.4

## 1. Kết nối và Stop

- `Connector.revoke()` đóng toàn bộ socket TCP hiện hoạt của phiên được cấp quyền; trước đây chỉ đặt `target=null` nhưng không ngắt phiên mạng cũ.
- Đăng ký socket **trước** khi `connect()` để Stop có thể hủy cả lần quay số TCP đang chờ. `SocketAdapter.close()` giải phóng socket ra khỏi danh sách theo dõi.
- `LegacyBotRuntime` thêm `reserveStart()` và `state()` để xử lý nhấn Chạy liên tiếp, hiển thị trạng thái khi tải cấu hình bất đồng bộ.
- `BotForegroundService` tự theo dõi worker và dừng Service sau khi worker kết thúc; Stop cố gắng ngắt network ngay, không phải đợi hàng đợi IO.
- UI mới hiển thị tối đa 4 dòng log gần đây và bắt lỗi Android từ chối gọi Foreground Service.

## 2. RMS bền vững, có mã hóa

- `RecordStore` hỗ trợ Backend; duy nhất `farm2x-teacher` và `farm2x-stats` có thể tồn tại trên disk.
- `EncryptedRmsBackend` mã hóa dữ liệu bằng AES-GCM với khóa Android Keystore, lưu ciphertext trong app-private SharedPreferences, bật từ `AvatarBotApplication`.
- `farm2x-config` (chứa username/password từ JAR) vẫn ở RAM; tài khoản Android đã dùng Keystore từ v0.2.
- Binary snapshot RMS v1 có kiểm tra header, số record, kích thước, id; dữ liệu hỏng được báo lỗi thay vì ghi đè im lặng.
- Hạn chế snapshot ở 2 MiB và 20.000 record để tránh tiêu tốn bộ nhớ quá mức.

## 3. Hành vi Auto và cấu hình

- `LegacyBotRuntime.createConfig()` tách việc ánh xạ cờ mode để test offline.
- Tắt `fishFarmIntervalMin` và `diamondFarmIntervalMin` theo mặc định khi chạy qua Android, tránh JAR bất ngờ quay sang Farm sau 30 phút.
- Khi `Thu khế` được chọn cùng Farm, màn Auto từ chối chạy vì chưa xác minh tương đương với `autoHarvestLove` của JAR.
- `LegacyBotRuntime.logs()` bổ sung che username và password của worker đang chạy. Tab Logs tiếp tục che mọi tài khoản trong kho.

## 4. Kiểm thử

- `LifecycleOfflineTest`: chặn Start trùng, hủy Start, ngắt `Socket.read` khi Stop, cấm mở lại socket sau revoke.
- `RmsPersistenceOfflineTest`: chỉ lưu Teacher/Stats, không lưu Config (mật khẩu), roundtrip qua giả lập tắt process, bản ghi lỗi và xóa dữ liệu.
- `ConfigModeOfflineTest`: xác nhận chính xác 4 mode và chặn chuyển Farm ngoài ý muốn.
- `verify_kotlin_psi.kt`: kiểm tra cú pháp 7 file Kotlin từ CLI không có Android SDK.
- Workflow GitHub in checksum SHA-256 của APK khi build thành công.

## Điều chưa kiểm thử

Android Gradle semantic compile/DEX/APK, startup trên Android, Keystore trên điện thoại thật, kết nối login/Auto server game thật. Các phần này không thể xác nhận chỉ từ JVM mocks.
