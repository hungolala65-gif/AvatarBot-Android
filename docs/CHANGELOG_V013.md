# AvatarBot Android v0.13 — sửa Setup theo JAR gốc

## Căn cứ

Đọc trực tiếp bytecode của `Bot2X-SuperLite.jar` (class `farm2x.Config`, `farm2x.Bot`, `farm2x.BotWorker`) và đối chiếu mã `MainActivity`, `SecureConfigStore`, `BotForegroundService`, `LegacyControlPanel`, `LegacyBotRuntime` trong v0.12. Không truy cập hay thử đăng nhập server thật.

## Sửa thiết lập gây hiểu lầm

- **Thu hoạch/Tưới/Vật nuôi/Thu khế:** JAR không có bốn công tắc độc lập tương ứng. Trước đây UI cho bật/tắt nhưng Service lại chặn cả Farm. v0.13 bỏ các công tắc thiếu ánh xạ khỏi màn hình thiết lập. Nếu một tài khoản cũ đã lưu tổ hợp không hỗ trợ, Farm vẫn bị chặn để tránh chạy ngoài ý muốn; nút **Đặt về mặc định JAR** cho phép người dùng chủ động sửa.
- **Gieo:** vẫn hỗ trợ `preferred`, `harvest_replay` hoặc `off` (tắt gieo mà không tắt cả Farm).
- **Bán vật nuôi:** JAR `Config.animalSellMode` mặc định là `late` và có thể tự bán vật nuôi. Bytecode `Bot.animalCare` kiểm tra chuỗi *không rỗng* để cho phép bán vật nuôi thịt, còn `Bot.eggCare` kiểm tra `late` để bán vật nuôi đẻ trứng. Vì thế v0.13 đặt `animalSellMode=""` để mặc định không tự bán; chỉ khi người dùng bật tùy chọn thì đặt `late`. **Không dùng chuỗi `off`** vì vẫn không rỗng và có thể gây bán.
- **Kim cương:** JAR `Config.load()` đặt `diamondSellKCX=true` và `diamondSellNHB=true` theo mặc định. Android v0.13 chủ động đặt cả hai thành false; chỉ khi người dùng chọn thì mới bật bán theo loại. Cấu hình không mang sang chế độ khác.
- **Tính năng mua/bán khác:** ép `autoSell=false`, `animalBuyMode="off"`, `autoStarUpgrade=false`, `noLuongBuy=true` để tránh mua, bán, tiêu Lượng và nâng cấp bất ngờ từ cấu hình JAR/RMS cũ.

## Thiết lập bổ sung có căn cứ bytecode

- **Fishing:** `Config.fishingRounds` = `0` (không giới hạn trong một đợt), hoặc 5/10/20/50/100 vòng trong một đợt. `BotWorker` tiếp tục lặp khi đợt kết thúc, nên đây **không phải** giới hạn tổng số lượt cho cả phiên chạy.
- **Teacher:** `Config.teacherArea` = -1 (chọn tự động) hoặc 0..99, theo chính phép giới hạn trong `Bot.runTeacher`. Chưa xác thực ID nào có hiệu lực với server thật.
- Cả ba nhóm tùy chọn được lưu AES-GCM trong `AutoOptions`, được nạp lại, và chuyển qua `BotForegroundService` đến `LegacyBotRuntime.createConfig`.

## Kiểm thử

- Bổ sung `AdvancedOptionsOfflineTest`: bốn chế độ, các mặc định an toàn, chọn bán từng loại, bán vật nuôi có lựa chọn, giá trị ngoài khoảng, và chính sách tương thích Farm.
- `bash tools/verify_offline.sh` gồm Go race/vet/cross compile, Kotlin import/redaction/TCP/SOCKS5/PSI, và Java ME giả lập handshake/LoginSuccess/LoginFail/lifecycle đều đạt.
- **Chưa chạy `assembleDebug` trên Android SDK** và chưa kiểm chứng hành động Auto với server game thật. Không được tuyên bố đây là bản dùng được 24/7.

## Tồn tại

1. Chưa kiểm thử đăng nhập thực, phiên bản client JAR và tác vụ nông trại/câu cá/giáo viên/kim cương với server thật.
2. JAR dùng trạng thái toàn cục, nên tối đa 1 tài khoản đồng thời.
3. Cần SDK Android/Gradle hoặc GitHub Actions hoạt động để build APK v0.13. Lần thử kết nối GitHub trước đó bị `403: Your account was suspended`.
4. Còn thiếu các cấu hình PC không tồn tại trong JAR và chưa kiểm chứng hành vi mua/bán/nấu ăn trên server.
