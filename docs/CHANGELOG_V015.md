# v0.15 — Changelog kỹ thuật / trạng thái

## Đã triển khai trong mã nguồn

1. **4 trang Auto riêng theo từng tài khoản:** Farm, Fishing, Teacher, Diamond; chuyển bằng tab/chip ngang. Từ tab Auto hoặc nút Setup trong danh sách Tài khoản. Cấu hình lưu `BotAccount.options` trong AES-GCM Android Keystore; sửa tài khoản A không thay đổi B.
2. **Farm:** chế độ gieo `preferred`/`harvest_replay`/`off`, `preferredSeedID` (-1 mặc định), `targetLandCount` (0 mặc định), lái buôn, nấu ăn, 5 cờ nhiệm vụ/điểm danh, tùy chọn bán vật nuôi già. Không giả vờ hỗ trợ công tắc tưới/thu hoạch riêng hoặc Thu khế PC.
3. **Fishing:** `fishingRounds` (0 không giới hạn), `fishingMap` (mặc định 14), `fishingArea` (-1), `fishingBaitID` (448), `fishingRodID` (442). Các mã là cấu hình Java ME; chưa xác minh khả năng sử dụng trên server hiện tại.
4. **Teacher:** `teacherArea` (-1..99), trạng thái CAPTCHA bên ngoài bị chặn và cảnh báo.
5. **Diamond:** `diamondSellKCX`/`diamondSellNHB` bật thủ công (mặc định tắt), `diamondSellDurationMin` (3), `diamondSellIntervalMs` (200); không tự chuyển Farm.
6. **Chung cho tài khoản:** thời gian nghỉ chu kỳ, thử lại worker, WakeLock tùy chọn. Lưu trước khi Start; cảnh báo thay đổi chưa lưu và hoàn tác.
7. **Lịch sử thực thi theo tài khoản:** `SecureExecutionJournal` mã hóa lưu UUID, mode, giai đoạn, timestamp (giới hạn 150) với các sự kiện START_REQUESTED, WORKER_STARTED, START_FAILED, WORKER_EXITED, WORKER_ERROR, RETRY_SCHEDULED, WAIT_NETWORK, STOP_REQUESTED, LOGIN_ACCEPTED, LOGIN_NOT_CONFIRMED, LOGIN_ERROR. Dữ liệu không chứa plaintext credential, không tạo thống kê hoạt động giả.
8. `ActiveAutoSession`: chỉ đánh dấu **phiên chạy trong tiến trình hiện tại**, không dùng để suy ra login/Auto thành công; chỉ lấy log JAR gần đây cho đúng tài khoản.
9. `LegacyAdvancedSetup.java` ánh xạ các trường nâng cao vào `farm2x.Config` **chỉ trong mode tương ứng**, có giới hạn kiểm tra và bài thử cross-mode.

## Đã kiểm thử offline

- `bash tools/verify_legacy.sh`: PASS, bao gồm `AdvancedSetupV15Test` xác minh mode-mapping và giới hạn từng trường; xác thực mock LoginSuccess/Fail, SOCKS5, retry, Stop.
- Kotlin CLI: `AccountImporter`, `GameCities`, `DiagnosticRedactor`, `KotlinNetworkProbe` PASS.
- Kotlin PSI: parse 11 Kotlin app source files PASS; **không** chứng minh Gradle build/semantic/typecheck.
- Go: `go test -race`, `go vet`, `GOOS=android GOARCH=arm64` PASS.

## Những điều chưa chứng minh / giới hạn

- **Chưa biên dịch APK v0.15** do môi trường làm việc thiếu Android SDK/Gradle và kết nối GitHub đã trả `403 Your account was suspended`.
- Chưa thử Android trên máy thật; chưa thử tài khoản với server thật; chưa xác minh kết quả cụ thể của Auto trong game.
- JAR Java ME dùng global state, chỉ chạy 1 tài khoản tại cùng thời điểm; chưa clone đầy đủ chức năng PC như VPS nodes, nhiều bot song song.
- Chính sách giới hạn mã bản đồ, vật phẩm là giới hạn **đầu vào bảo thủ của app**, không phải xác minh mọi ID đó tồn tại trên server.
- Kết quả LoginSuccess lịch sử thuộc lần thử trước; khi đổi mật khẩu, chỉ nút thử đăng nhập hiện tại có thể cho biết trạng thái credential mới.
- Thuộc tính hoạt động 24/7 phụ thuộc hệ điều hành và điều kiện thiết bị; WakeLock không bảo đảm hoạt động liên tục.
