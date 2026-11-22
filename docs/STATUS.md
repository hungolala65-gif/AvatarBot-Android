# Ma trận trạng thái — AvatarBot Android v0.3 (18/11/2026)

| Thành phần | Trạng thái | Mức kiểm chứng |
|---|---|---|
| JAR Java ME 60 classes | Đã phân tích, xác nhận bytecode Java 8 MIDP | `javap`/ZIP audit |
| JAR headless 50 classes | Đã đóng gói kèm Android | Kiểm tra các `farm2x` refs đầy đủ |
| MIDP SocketConnection / Connector | Đã viết TCP + SOCKS5 và allowlist | 41 assertions offline |
| RMS memory bridge | Đã viết | `Config.load()` và mock login PASS |
| JAR original BotWorker | Đã nối với Android Service | Java compile PASS; device chưa test |
| JAR bootstrap / Login payload | Đã chạy trên server mock local | 4 bootstrap packets + login + LoginFail PASS |
| UI 5 tab + Java mode panel | Đã viết | Kotlin PSI PASS; Gradle build chưa chạy |
| Keystore Android | Mã nguồn v0.2 giữ lại | Chưa chạy trên thiết bị |
| Auto Farm | Chạy qua JAR mode=0 khi người dùng chọn | **Chưa xác minh server thật** |
| Auto Fishing | JAR mode=1, solver bị chặn | **Có thể phụ thuộc solver; chưa xác minh** |
| Auto Diamond | JAR mode=2 | **Chưa xác minh server thật** |
| Auto Teacher | JAR mode=3, RAM-only RMS | **Chưa xác minh server thật; không giữ lịch sử lâu dài** |
| 2 cờ merchant/cooking | Đã ánh xạ vào Config JAR | Chưa xác minh trong game |
| Cờ thu hoạch/gieo/tưới/vật nuôi/thu khế | Chỉ lưu; **chưa ánh xạ** | Chặn Farm nếu nhóm 4 đầu bị tắt |
| Đa tài khoản đồng thời | **Chưa hỗ trợ** | Chỉ cho chạy 1 worker do globals |
| Go Core v0.2 | Giữ lại, Go unit tests PASS | Go auto vẫn chỉ stub |
| Debug APK | Script và CI cập nhật (không cần gomobile) | **Chưa build** |
| Thiết bị Android thật và server live | Chưa test | Không khẳng định hoạt động |

Không nhầm lẫn: **JAR bot code đã tích hợp và kiểm thử offline** khác với **đã có APK chạy Auto thành công trên server thật**.
