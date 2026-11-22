# Hướng dẫn biên dịch AvatarBot v0.13

Dùng Android Studio mới có JDK 17, Android SDK Platform 35, Gradle 8.9. Mở thư mục `android/`, tải dependencies từ Gradle Maven repositories, rồi chạy `assembleDebug`. Trên Linux (khi đã có Gradle trong PATH), tại `android/` có thể chạy `gradle wrapper --gradle-version 8.9 && ./gradlew --no-daemon assembleDebug`. File kết quả: `android/app/build/outputs/apk/debug/app-debug.apk`.

Trước đó, có thể chạy `bash tools/verify_offline.sh` ở root để kiểm tra Go, Kotlin ở mức cú pháp/JVM và JAR trên fake server. Không sử dụng dữ liệu tài khoản thật trong bài test.

GitHub Actions `.github/workflows/build-apk.yml` đã được cập nhật để lưu APK và checksum thành artifact `avatarbot-v0.13-debug-apk` khi thành công; người dùng phải đẩy các **tệp đã giải nén**, không chỉ ZIP, lên repository có quyền Actions. Repository cũ bị lỗi 403 nên chưa thể chạy workflow từ phiên này.

Không chia sẻ APK/JAR công khai khi chưa rõ quyền phân phối mã Java ME gốc. APK debug chưa phải bản phát hành chính thức; hãy cài trên máy thử và lưu tài khoản trước khi gỡ APK cũ do có thể khác khóa ký.
