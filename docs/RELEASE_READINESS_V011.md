# AvatarBot Android v0.11 — đối chiếu PC và mức sẵn sàng phát hành

**Nguồn kiểm tra trực tiếp:** `AvatarBot-Fix-Full.zip` (3 tệp: tài liệu, Windows EXE, Linux ELF), `Bot2X-SuperLite.jar` (Java ME bytecode) và mã Android v0.10 đã được nâng cấp thành v0.11. Bản PC là Go, **không kèm mã nguồn Go**. JAR là một triển khai khác và không thể coi là bản sao đầy đủ của PC.

## Chức năng

| Tiêu chí | Thực tế v0.11 | Cần thêm |
|---|---|---|
| Biên dịch APK | **v0.10 đã build APK debug thành công**; v0.11 chưa build Android | Chạy GitHub Actions khi quyền repository hoạt động trở lại |
| Giao diện / lưu tài khoản | Compose + AES-GCM Android Keystore | Kiểm thử trên thiết bị, nâng cấp/khôi phục dữ liệu |
| Kết nối game | JAR TCP/SOCKS5, handshake và Login đã test với server giả lập | Kết quả từ server thật; không dùng mật khẩu thật trong báo cáo |
| Nông trại | Có JAR Farm worker | Ánh xạ chính xác từng thao tác và thử nghiệm thực tế |
| Câu cá, Giáo viên, Kim cương | Có JAR worker | Kiểm thử thực tế theo từng tác vụ |
| Chạy nền | Foreground Service, WakeLock tùy chọn, retry giới hạn | Kiểm thử 8–24 giờ, thay đổi mạng, pin, màn hình tắt |
| Chu kỳ | v0.11 chọn 15/30/60/120/300 giây theo tài khoản | Kiểm tra trong thiết bị / game thật |
| Nhiều tài khoản song song như PC | **Chưa có** | Tách trạng thái toàn cục của JAR hoặc xây lại worker |
| Node VPS và bảng điều khiển web PC | **Chưa có** | Thiết kế lại nếu vẫn cần tương đương PC |
| Phát hành chính thức | Debug APK, bản quyền JAR/PC chưa được xác minh | Khóa ký chính thức, chính sách foreground, thử nghiệm thực tế, quyền phân phối |

## Tỷ lệ (đánh giá chủ quan, không phải tỷ lệ test coverage)

- **Khả năng tạo file APK debug từ mã nguồn:** cao (bản v0.10 đã build, v0.11 chưa xác nhận).
- **Mức sẵn sàng để phát hành thử cho người dùng kỹ thuật:** khoảng 60–70% *nếu* v0.11 build thành công; chưa kiểm thử máy thật nên chỉ dùng để thử.
- **Mức sẵn sàng sản xuất/phát hành rộng rãi như một bot tương đương PC:** khoảng **30–40%**, tạm lấy **35%**. Không thể nâng con số này chỉ bằng test offline vì chưa có xác nhận đăng nhập/game thật và thiếu chạy nhiều tài khoản.

Để nâng lên 80–90% cần ít nhất: (1) APK v0.11 build hợp lệ, (2) test login với 2 thành phố dùng tài khoản thử nghiệm không nhạy cảm, (3) test mỗi chế độ Auto và Stop/Cancel, (4) test qua tắt màn hình/mất kết nối 8–24 giờ, (5) kiểm tra nguồn gốc và quyền dùng JAR/PC, (6) kiểm thử cập nhật ứng dụng và chữ ký ổn định, (7) giải quyết đa tài khoản nếu đó là chức năng bắt buộc.

**Giới hạn môi trường lần này:** Kết nối GitHub trả `403: Your account was suspended`. Môi trường làm việc không có Android SDK và Gradle nên không thể tạo v0.11 APK trong phiên này. Đây không phải lỗi Java/Kotlin đã được kiểm chứng; chưa có build Android mới.
