# AvatarBot Android v0.8 — trạng thái đã kiểm chứng

- Android app dùng Java ME JAR headless làm bộ chạy chính ở cả tab Tổng quan và Auto.
- Các tác vụ từ JAR: Nông trại, Câu cá, Giáo viên và Kim cương (chưa kiểm thử máy chủ game thật).
- Năm cờ nhiệm vụ JAR được ánh xạ từ cấu hình mã hóa: dailyQuest, hugoQuest, eventQuest, farmMission, farmCheckin.
- Không còn nút Go bị khóa trong Tổng quan. Go Core vẫn là bản thử nghiệm không có worker game, không được quảng bá là đã hoàn thiện.
- Mã nguồn vẫn chưa ngang đầy đủ bản PC Go về số chức năng và vận hành nhiều tài khoản đồng thời.
- Kiểm thử offline Java ME, 32 tổ hợp cờ, Kotlin syntax, Go đạt. Android CI bắt buộc phải xác minh APK sau khi build.

**Cảnh báo cập nhật debug APK:** GitHub Actions có thể ký hai bản APK bằng khóa debug khác nhau; điện thoại có thể yêu cầu gỡ bản cũ, khiến dữ liệu lưu trong Keystore mất. Không xóa ứng dụng cũ khi chưa lưu lại thông tin tài khoản bằng cách an toàn.
