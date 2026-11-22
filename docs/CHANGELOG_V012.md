# Thay đổi AvatarBot Android v0.12

| Hạng mục | Đã sửa | Bằng chứng | Hạn chế |
|---|---|---|---|
| Không mạng khi worker thoát | Trạng thái `network_wait`, chờ tối đa 30 phút, callback + polling dự phòng | `NetworkWaitPolicyOfflineTest` | Chưa đo tiêu thụ pin/khả năng bật lại trên máy thật |
| Ngăn hao lượt retry | Khi offline thì giữ nguyên bộ đếm; sau mạng hồi phục mới lập lịch retry | `NetworkWaitPolicyOfflineTest` | Nếu mạng rớt trong khoảng chờ, bộ đếm được hoàn trả khi lịch thử lại chưa được thực thi |
| Hủy thử lại bằng nút Dừng | Dọn timer/callback, tắt wait và đóng socket | `ProbeIdentityOfflineTest`, `LifecycleOfflineTest` | Hệ điều hành vẫn có thể chấm dứt toàn bộ ứng dụng |
| Kết quả Login độc lập | Chỉ hiện kết quả khi tên tài khoản, mật khẩu, endpoint cùng khớp | `ProbeIdentityOfflineTest` | Không chứng minh LoginSuccess từ server thật |
| Chế độ gieo | `preferred`, `harvest_replay`, `off` ánh xạ `farm2x.Config.seedMode` | `SeedModeOfflineTest`, javap bytecode của JAR | Không có bằng chứng Farm live; tắt gieo có thể vẫn thu hoạch |
| Sao chép chẩn đoán | Nút sao chép từ Logs, che dữ liệu phổ biến | `verify_diagnostic.kt` | Cần kiểm tra thủ công; không bảo đảm che mọi chuỗi nhạy cảm |
| Version | `versionCode=12`, `versionName=0.12.0` | Kiểm tra mã nguồn | Chưa build Gradle APK v0.12 |

## Trạng thái không thay đổi

- Chỉ một phiên Java ME Worker do các trạng thái tĩnh trong JAR.
- Thiếu mã nguồn backend Go của bản PC; không thể cam kết tương đương 1:1.
- Không hỗ trợ đầy đủ các cờ Farm độc lập, VPS nodes hoặc PC vault.
- Không xác nhận đăng nhập, chống CAPTCHA hay Auto trên server thật. Không cam kết treo 24/7.
- Không nên coi bản debug là bản phát hành công khai sẵn sàng cho người dùng cuối.
