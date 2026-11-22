# AvatarBot Android Native v0.6 — CI & service fixes

# AvatarBot Android Native v0.5 — JAR core và tính ổn định

Đây là dự án Android Kotlin/Jetpack Compose có tích hợp **bytecode thật** từ file `Bot2X-SuperLite.jar` do người dùng cung cấp. Không cần PC/VPS để điều khiển trong kiến trúc triển khai. Không phải bản APK sẵn cài đặt: **chưa build Android Gradle/DEX và chưa kiểm thử trên máy thật hoặc server game thật**.

## Những thành phần có trong v0.5

- Android UI: 5 tab (Tổng quan, Tài khoản, Auto, Kết nối, Logs); thêm/sửa/xóa, nhập nhiều tài khoản, cấu hình riêng, mật khẩu mã hóa Android Keystore.
- **Java ME core thật**: `android/app/libs/bot2x-headless.jar` giữ nguyên 50 class bytecode không thuộc giao diện MIDP từ JAR gốc; xác minh từng class trùng byte-for-byte với JAR gốc.
- Nút Chạy/Dừng 1 worker từ 4 chế độ `Farm / Fishing / Diamond / Teacher`, thông qua Android Foreground Service. Đọc tài khoản theo ID từ kho mã hóa; không truyền mật khẩu qua Intent.
- Cầu nối `javax.microedition.io`: chỉ cho kết nối host:port đã cấp phép, TCP/SOCKS5, có timeout và **đóng socket đang mở khi Stop**.
- Bảo vệ vòng đời khởi chạy bằng ticket: **Dừng** khi đang đọc kho tài khoản không để worker chạy lại ngoài ý muốn. SOCKS5 có timeout khi chờ xác thực proxy.
- Trạng thái `starting/running/stopped/failed` và giám sát để tự dừng Foreground Service khi worker kết thúc.
- Cầu nối `javax.microedition.rms`: cấu hình JAR chứa mật khẩu vẫn **chỉ nằm trong RAM**; dữ liệu `farm2x-teacher` và `farm2x-stats` được lưu qua Backend Android **AES-GCM / Android Keystore**. Không tự xóa snapshot hỏng.
- Khi chọn Câu cá/Kim cương, Android tắt lịch tự chuyển sang Farm sau 30 phút vốn bật mặc định trong JAR. Chế độ Nông trại từ chối khởi chạy nếu chọn các cờ tác vụ phụ chưa ánh xạ chính xác.
- Chức năng xử lý bot Java ME được giữ nguyên; API Go thử nghiệm nằm trong `go-mobilecore/` để tham khảo nhưng **không được dùng cho chế độ JAR**.

## Kiểm thử đã thực hiện

Chạy ở thư mục gốc:

```bash
bash tools/verify_offline.sh
```

Kiểm thử offline bao gồm: Go tests/race/vet/build ARM64, nhập tài khoản Kotlin, TCP/SOCKS5 Kotlin, cú pháp Kotlin PSI, 44 assertions JAR chính, handshake + login giả lập, 8 assertions RMS persistence, vòng đời khởi chạy/đóng socket, ánh xạ các chế độ Auto. **Đều PASS trong môi trường JVM/Go/Kotlin CLI**.

## Cách build APK

**Có hai phương án:**

1. Mở thư mục `android/` với Android Studio đã cài SDK 35 + JDK 17; đồng bộ Gradle, chọn Build > Build APK(s). Nếu Android Studio không tự nhận Gradle wrapper vì repo không chứa `gradle-wrapper.jar`, hãy cài Gradle 8.9 và chạy `gradle wrapper --gradle-version 8.9` từ thư mục `android/`, sau đó chạy `./gradlew assembleDebug`.
2. Đưa cấu trúc repo này lên GitHub và chạy workflow `.github/workflows/build-android.yml`. Workflow tự cài SDK/Gradle và sẽ xuất **APK nếu Gradle build thành công**. Chưa có bằng chứng workflow đã được chạy thành công.

APK debug nếu build thành công nằm tại `android/app/build/outputs/apk/debug/app-debug.apk`.

## Giới hạn quan trọng

- **Chưa có APK được biên dịch và cài thử** trong môi trường hiện tại (không có Android SDK/Gradle); chỉ có mã nguồn và kết quả kiểm thử offline.
- Chưa kiểm chứng đăng nhập thành công hoặc các chế độ Auto với server game thật. Test login chỉ chạy server giả lập và phản hồi thất bại mô phỏng.
- JAR có trạng thái tĩnh (`Stats`, `TeacherStore`, `Log`): **chỉ 1 tài khoản/mode chạy cùng lúc**; thống kê/lịch sử giáo viên không tách riêng theo tài khoản.
- Solver bên ngoài vẫn bị từ chối bởi whitelist; những chức năng phụ thuộc solver có thể không hoạt động.
- Các cờ thu hoạch/gieo/tưới/vật nuôi chưa thể bật/tắt riêng trong JAR; bật `Thu khế` không ánh xạ nên ứng dụng **chặn** Farm nếu cờ này bật.
- Không đảm bảo hoạt động 24/7 do giới hạn pin/Foreground Service Android hoặc tương thích với chính sách của server game.
- TCP/XOR của game gốc không phải giao thức TLS; tránh dùng tài khoản quan trọng khi chưa xác minh endpoint đáng tin cậy.

Chi tiết kỹ thuật, thay đổi và quy trình QA: `docs/STATUS_V04.md`, `docs/CHANGELOG_V04.md`, `docs/TEST_RESULTS_V04.txt`, `docs/BUILD_AND_LIMITATIONS.md`.

### Phát hành v0.5

Đây vẫn là **mã nguồn đã kiểm thử offline**, không phải file APK. Đọc `docs/CHANGELOG_V05.md` và `docs/BUILD_AND_LIMITATIONS.md`. Muốn tạo APK, chạy GitHub Actions `Build AvatarBot Android (v0.5 Java ME core)`; nếu build thất bại tải artifact diagnostics để phân tích. Không được báo cáo đã build/cài đặt hoặc Auto đã chạy với server thật khi chưa có log/thiết bị kiểm chứng.

## v0.6 updates

See `docs/CHANGELOG_V06.md` and `docs/GITHUB_NEXT_STEPS.md`. Adds service Stop/Start race protection, notification stop action and full offline tests in the GitHub workflow. APK build and real-server automation remain unverified.

## v0.10 — Kiểm tra đăng nhập trước khi bật Auto

Bản v0.10 thêm **Kiểm tra đăng nhập** trực tiếp bằng `farm2x.Bot.connectLogin` trong JAR, tách riêng khỏi việc kích hoạt các tác vụ Auto. Giao diện báo kết quả LoginSuccess / chưa xác thực được / lỗi kết nối, không ghi mật khẩu vào kết quả. Bộ test JVM thêm một server giả lập trả LoginSuccess và LoginFail. Xem `docs/LOGIN_TEST_V09.md`.

APK được build qua GitHub Actions; build thành công không chứng minh đăng nhập trên game thật. Mục tiêu tiếp theo phụ thuộc vào kết quả kiểm tra đăng nhập từ thiết bị của người dùng.


### v0.10 Long-running mode
See [docs/STATUS_V010.md](docs/STATUS_V010.md) for the optional wake lock and bounded worker restart, plus limitations.
