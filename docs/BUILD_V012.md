# Build và kiểm tra v0.12

1. Giải nén ZIP nguồn vào thư mục trống. File gốc `Bot2X-SuperLite.jar` đã được trích ra thành `android/app/libs/bot2x-headless.jar`, không cần tải lại.
2. Chạy `bash tools/verify_offline.sh` và chỉ tiếp tục nếu toàn bộ kiểm thử đạt.
3. Sử dụng Android Studio với JDK 17 / Android SDK 35; mở riêng thư mục `android/`, chạy Gradle 8.9 và `assembleDebug`.
4. APK debug sẽ có tại `android/app/build/outputs/apk/debug/app-debug.apk`. Chỉ cài APK khi file thực sự tồn tại, không thay đuôi ZIP.
5. Nếu build bằng GitHub Actions, đưa **nội dung giải nén** lên nhánh repo, chạy `.github/workflows/build-apk.yml`. Repo `hungrose15620-wq/AvatarBot-Android` hiện trả `403: Sorry. Your account was suspended`, không thể chạy CI qua kết nối cho đến khi GitHub khôi phục quyền.
6. Để thử trên máy, bắt đầu bằng một tài khoản thử nghiệm tại thành phố đúng. Ở tab Kết nối, thử TCP rồi ở Tổng quan nhấn Kiểm tra đăng nhập. Chỉ dùng Auto khi kết quả LoginSuccess có bằng chứng. 
7. Khi bật thử lại và có phiên worker đang chạy, ngắt Wi-Fi/dữ liệu di động. Trạng thái `network_wait` chỉ xuất hiện nếu worker thực sự thoát; hệ thống chờ mạng hồi phục không tiêu lượt retry. Bấm Dừng phải chặn thử lại.
8. Để báo lỗi, vào Logs → Làm mới → Sao chép nhật ký đã che dữ liệu, **kiểm tra lại** trước khi gửi. Không gửi mật khẩu hoặc danh sách tài khoản.

## Cảnh báo

- Tắt Gieo hạt vẫn có thể thu hoạch và bỏ trống đất. Hãy chỉ bật sau khi hiểu rõ hành vi.
- Phát hành chính thức cần kiểm tra bản quyền/JAR, chữ ký phát hành, chính sách Foreground Service, thử nghiệm 8–24 giờ trên điện thoại và xác minh giao thức server.
- APK debug v0.10 được ký bằng debug key; build ở máy khác có thể không cài đè. Trước khi gỡ bản cũ, ghi lại thông tin tài khoản vì Android Keystore có thể mất dữ liệu sau khi gỡ.
