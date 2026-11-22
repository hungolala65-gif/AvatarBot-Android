# Tiến độ AvatarBot Android Native v0.5

- Java ME Core 50 class bytecode: tích hợp; so khớp 50/50 class với JAR người dùng gửi.
- Worker Auto 4 mode: có đường gọi mã gốc từ Android UI đến Service, được kiểm thử trên JVM với server giả; **chưa chứng minh đăng nhập thành công hoặc Auto trên server game thật**.
- An toàn vòng đời: chống chạy lại sau khi Stop trong lúc đọc tài khoản; kiểm thử mới PASS.
- SOCKS5/TCP: xác thực proxy có timeout và Stop đóng socket ngay; kiểm thử proxy giả PASS.
- Keystore: có mã triển khai; kiểm thử offline chỉ xác nhận backend giả và logic RMS, **chưa chạy Android Keystore thực tế**.
- Giao diện Compose, Foreground Service: đã viết mã, Kotlin PSI phân tích cú pháp thành công; **chưa có semantic build/lint/device run**.
- Build APK: cần chạy Android SDK + Gradle trên Android Studio hoặc GitHub Actions. Hiện **không có APK đã được tạo**.

Để build trên GitHub: đẩy toàn bộ nội dung thư mục chứa `.github/`, `android/`, `tools/` lên repository → Actions → chọn **Build AvatarBot Android (v0.5 Java ME core)** → Run workflow → nếu thành công tải artifact **avatarbot-android-v0.5-debug**. Nếu fail, lấy artifact **avatarbot-v0.5-build-diagnostics** để phân tích lỗi.
