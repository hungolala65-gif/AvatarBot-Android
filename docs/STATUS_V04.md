# Trạng thái dự án v0.4 — 18/11/2026

| Hạng mục | Có mã nguồn | Đã kiểm thử | Còn thiếu |
|---|---|---|---|
| UI Kotlin Compose 5 tab | Có | Kotlin PSI syntax | Android Gradle compile, emulator/phone |
| JAR headless 50 class | Có, nguyên bytecode JAR người dùng gửi | javac/JVM và so khớp bytecode | D8/dex, tính tương thích Android runtime |
| Quản lý account mã hóa | Có | logic import Kotlin | Keystore Android thực tế, recovery UX |
| TCP / SOCKS5 / KeyRequest | Có | JVM mock server | network server game thật |
| Dừng TCP đang hoạt động | Có | Mock socket read đang chờ | Foreground lifecycle thực tế |
| Auto Farm | Gọi JAR mode 0 | Tạo config, thử login fail | Server live và xử lý tình huống thực tế |
| Auto Fishing | Gọi JAR mode 1, không tự Farm | Mode mapping | Server live, xử lý phụ thuộc solver |
| Auto Diamond | Gọi JAR mode 2, không tự Farm | Mode mapping | Server live |
| Auto Teacher | Gọi JAR mode 3 | Teacher RMS roundtrip | Nội dung QA thực tế, server live |
| RMS Teacher/Stats | Backend AES-GCM Android | Kiểm thử snapshot trên JVM (backend giả) | Android Keystore encryption device tests |
| Foreground Service | Có | Kotlin PSI syntax | Android lifecycle, background/pin testing |
| Đa tài khoản đồng thời | Chưa | Không áp dụng | Refactor tĩnh JAR (phức tạp) |
| Build debug APK | CI/workflow/script có | Chưa | SDK/Gradle và chạy CI/build thật |

**Không được mô tả v0.4 là APK hoàn chỉnh:** đây là mã nguồn tiến gần hơn tới bản Android chạy độc lập nhưng chưa được biên dịch thành APK và chưa được xác minh với server thực.
