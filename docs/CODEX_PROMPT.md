# Prompt tiếp tục phát triển AvatarBot v0.3 bằng Codex

Copy nội dung bên dưới kèm toàn bộ source v0.3. Không gửi tài khoản/mật khẩu thật:

```text
Bạn là kỹ sư Android/Kotlin/Java chuyên port Java ME MIDP sang Android.
Tôi đã cung cấp AvatarBot Android Native v0.3. Trong android/app/libs có
bot2x-headless.jar chứa 50 class core từ Bot2X-SuperLite.jar (60 class).
Dự án đã tích hợp javax.microedition.io & javax.microedition.rms, Java
LegacyBotRuntime, Kotlin LegacyControlPanel và Android Foreground Service.

Ưu tiên:
1. Build APK thật bằng Gradle/JDK17/SDK35: sửa mọi lỗi Java/Kotlin/Compose,
   manifest, resource, dex, D8 và runtime; đừng chỉ kiểm tra syntax.
2. Chạy tools/verify_legacy.sh: phải PASS 44 assertions và mock login flow.
3. Thêm JVM integration tests cho các MIDP dependency còn lại và các mode.
4. Kiểm tra Foreground Service start/stop trên thiết bị thật (Android 8-15+),
   tối thiểu một tài khoản / một mode; không tự khởi chạy khi mở app.
5. Xác minh login với server do tôi có quyền truy cập ở môi trường thử nghiệm;
   không log/mã cứng password/token, không tự ý gửi tài khoản đến dịch vụ khác.
6. Chỉ ánh xạ các config flags khi xác minh chính xác hành vi từ bytecode.
   Nhóm farm toggles đang fail-closed; không tự bật tùy chọn chưa map.
7. `SolverClient` trỏ dịch vụ bên thứ ba: giữ network allowlist, KHÔNG bỏ
   chặn để vượt CAPTCHA hoặc chuyển dữ liệu khi chưa được người dùng cho phép.
   Có thể thiết kế yêu cầu nhập CAPTCHA thủ công nếu cần và được phép.
8. Giữ profile trong Android Keystore. Mọi nhật ký cần che username/password.
9. Không chạy đa tài khoản song song bằng JAR hiện tại do Log/Wallet/Stats/
   TeacherStore globals. Chỉ triển khai sau khi cách ly an toàn state.
10. Cập nhật docs/STATUS.md và chạy CI. Phân biệt tính năng đã kiểm thử trên
    mock, đã chạy Android, đã xác minh server thật, và chưa làm.

Mục tiêu APK Android độc lập, không cần PC/VPS. Tránh báo hoàn thành 100%
khi chưa kiểm thử đầy đủ các chức năng game thật.
```
