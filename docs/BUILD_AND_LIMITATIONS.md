# Build AvatarBot Android v0.5

## Build APK trên PC hoặc CI

Yêu cầu: JDK 17, Android SDK Platform 35 / Build Tools 35.0.0, Gradle 8.9, kết nối kho Google/Maven để tải plugin Android/Compose.

**Android Studio:** mở thư mục `android/` rồi chọn `Build > Build APK(s)`. Đây là mã nguồn dự án, không phải file APK đã sẵn có.

**Command line (đã có Gradle CLI):**

```bash
cd android
gradle wrapper --gradle-version 8.9
./gradlew --no-daemon assembleDebug
```

**GitHub Actions:** Upload tất cả file dưới thư mục dự án lên repo GitHub. Chạy Actions > `Build AvatarBot Android (v0.5 Java ME core)` > `Run workflow`. Nếu build thành công, tải artifact `avatarbot-android-v0.5-debug`.

Nếu thành công, file APK ở `android/app/build/outputs/apk/debug/app-debug.apk`.

## Chạy kiểm thử offline trước

```bash
bash tools/verify_offline.sh
```

Cần Go và Kotlin CLI nếu muốn chạy tất cả; `bash tools/verify_legacy.sh` chỉ cần JDK 17. Không dùng tài khoản thực hoặc truy cập server game trong kiểm thử.

## Lưu ý thiết bị

- Android 8.0+ (`minSdk 26`), build target Android 15 (`targetSdk 35`).
- Kết nối mạng và quyền thông báo có thể được hỏi tùy phiên bản Android.
- Foreground Service là `specialUse`; loại và khai báo này có thể cần điều chỉnh nếu phân phối qua Google Play. Không bảo đảm chạy 24/7 khi máy hạn chế pin.
- Dữ liệu Teacher/Stats dùng khóa Android Keystore; gỡ cài đặt/xóa data sẽ làm mất dữ liệu mã hóa (không có export/import).
- Chưa chạy build Android thực tế tại môi trường phân tích. Nếu Gradle báo lỗi, cần lấy log build để sửa tiếp.
- Cần kiểm tra server được phép sử dụng bot và không thử tài khoản có giá trị khi chưa kiểm chứng endpoint.

Xem `README.md`, `docs/CHANGELOG_V05.md`, `docs/STATUS_V05.md`.

## Nếu GitHub Actions báo lỗi

Download artifact `avatarbot-v0.5-build-diagnostics` (nếu có), lấy nội dung `android-build.log` và sửa lỗi Gradle/DEX thực tế trước khi phân phối APK. CI chưa từng được thực thi trong phiên này; workflow chỉ là cấu hình build, không phải bằng chứng APK đã build thành công.

**Lưu ý:** Để có APK thực sự cần một môi trường có Android SDK 35, Gradle 8.9 và Internet truy cập Google Maven/Maven Central. Môi trường phân tích mã hiện tại không có các điều kiện này. Không đổi phần mở rộng của JAR/ZIP thành APK.
