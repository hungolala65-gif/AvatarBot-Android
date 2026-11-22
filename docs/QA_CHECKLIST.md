# Checklist kiểm thử trước khi phát hành APK

## Test đã chạy ở môi trường phát triển

- [x] Go `go test ./...` — account validation, trực tiếp TCP, SOCKS5 giả lập, lifecycle start/stop, không mở auto khi thiếu protocol.
- [x] Go `go vet ./...` — không có cảnh báo.
- [ ] Gomobile tạo `.aar` Android arm64 (chưa có SDK/NDK ở đây).
- [ ] Gradle build `app-debug.apk` (chưa có Android SDK/Gradle ở đây).

## Kiểm tra thủ công trên điện thoại

- [ ] Android 10/12/13/14/15/16: mở UI, xoay màn hình, điều hướng giữa tab.
- [ ] Lưu account, đóng/mở app, kiểm tra vẫn còn; mật khẩu luôn bị che.
- [ ] Xóa/cập nhật/bật tắt account, không lộ password vào Logs hoặc Logcat.
- [ ] Cài AAR → tính năng thử TCP hoạt động; không có AAR → nút thử bị khóa.
- [ ] Thử host không mở cổng; timeout được thông báo an toàn.
- [ ] SOCKS5 hợp lệ/sai mật khẩu; dữ liệu user/pass không vào log.
- [ ] Thử máy bay/đổi WiFi sang 4G rồi quay lại.
- [ ] Cài lại app: xác nhận dữ liệu cũ có thể mất (Keystore).
- [ ] Worker thật: 1 và nhiều account, request cancel không treo quá lâu.
- [ ] Background/FGS: màn hình tắt, Doze, kill process, tự động khởi động lại **không được giả định**.
- [ ] Kiểm tra yêu cầu Play Store và điều khoản game trước khi phát hành.
