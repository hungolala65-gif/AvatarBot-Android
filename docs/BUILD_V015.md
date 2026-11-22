# Build AvatarBot Android v0.15

Từ thư mục gốc, chạy các kiểm thử offline (Java 17+, Kotlin CLI, Go):

```bash
bash tools/verify_offline.sh
```

Để tạo APK cài đặt cần **Android SDK Platform 35 + Build Tools 35.0.0 + JDK 17 + Gradle 8.9**, hoặc GitHub Actions kết nối lại được:

```bash
cd android
gradle wrapper --gradle-version 8.9
./gradlew --no-daemon --stacktrace assembleDebug
```

Sau khi lệnh thành công, kiểm tra `android/app/build/outputs/apk/debug/app-debug.apk`. Workflow `.github/workflows/build-apk.yml` lưu bản debug cùng checksum. **Lưu ý: ZIP nguồn không phải APK.** Chỉ cài bản debug trên tài khoản thử nghiệm; nếu gỡ APK cũ có thể mất dữ liệu Android Keystore. Cần quyền phù hợp để sử dụng/phân phối JAR.

Repository GitHub cũ hiện trả `403: Your account was suspended`, không thể push hoặc lấy artifact tại thời điểm kiểm tra. Không đưa mật khẩu game, proxy credentials hoặc log nguyên bản vào repo public.

Khi kiểm tra chức năng: ưu tiên tạo tài khoản thử → kiểm tra LoginSuccess → chạy từng mode riêng → kiểm tra log và trạng thái sau 5, 30, 120 phút → thử Stop, mất mạng và màn hình tắt. `WORKER_STARTED` chỉ xác nhận thread khởi chạy, không xác nhận thao tác game thành công.
