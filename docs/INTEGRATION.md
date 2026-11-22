# Hướng dẫn hoàn thiện Auto Game trên Android

## 1. Rào cản từ tập tin PC

Đầu vào chính xác là `AvatarBot-Fix-Full.zip`: hướng dẫn và hai binary Windows/Linux amd64. Go metadata có các tên module `internal/session`, `internal/proto`, `internal/farmer`, `internal/manager`, `internal/vault`, `internal/socks5`. Đây **chỉ là bằng chứng cấu trúc**, không phải code gốc. JavaScript đã phục hồi chỉ là UI khách, không thay thế game protocol. Không nên hứa chuyển đủ tính năng khi chưa có source/module tương đương.

## 2. Nơi ghép code gốc

- `go-mobilecore/gameworker_stub.go`: `newGameWorker() worker` hiện trả `nil`. Giữ nguyên until có worker thật. **Không trả fake worker** để nút Chạy sáng lên.
- `go-mobilecore/engine.go`: `Engine.Start()` sinh worker theo từng tài khoản enabled; `Engine.Stop()` hủy context và chờ worker. Bắt buộc worker.Run hủy nhanh khi `ctx.Done()` và mọi IO có timeout/đóng connection.
- `go-mobilecore/netprobe.go`: đã có dial TCP/SOCKS5. Có thể tái dùng trong `session` khi giao thức rõ ràng.
- `android/.../engine/GoMobileBridge.kt`: giao tiếp với AAR qua các phương thức `newEngine`, `setAccountsJSON`, `start`, `stop`, `probeJSON`, `stateJSON`, `capabilitiesJSON` (được tạo từ Go public API).

## 3. Module Go cần có (theo thứ tự)

1. **protocol/frame** — byte order, encode/decode gói tin, kiểm tra độ dài, giới hạn dữ liệu, lỗi frame.
2. **session** — TCP connect/handshake, login, quản lý timeout/reconnect, lựa chọn server theo city.
3. **credentials** — password game được lấy từ Android Keystore trong thời gian đăng nhập và truyền vào Go bằng API riêng; không ghi logs, không xuất trong `StateJSON`, xóa tham chiếu khi dừng. `SetAccountsJSON` đang từ chối trường `password` có chủ ý.
4. **farmer** — thu hoạch, tưới, gieo, chữa bệnh, bếp, v.v. theo phản hồi server.
5. **fishing / teacher / diamond** — tách worker riêng, có cấu hình và test thực tế.
6. **manager** — lịch chạy, concurrency, giới hạn kết nối, tính độc lập từng tài khoản và proxy.
7. **vault migration** — chỉ thực hiện khi giải mã được định dạng PC và người dùng có mật khẩu vault; không đoán cấu trúc.

## 4. Yêu cầu worker tương thích

Ví dụ phác thảo (*không phải code hoạt động với game*):

```go
type gameAdapter struct { /* session / proto / farmer của source thật */ }
func (g *gameAdapter) Run(ctx context.Context, account Account) error {
    // 1. Đọc credential qua cơ chế an toàn đã duyệt
    // 2. Dial timeout -> handshake -> login (từ protocol có nguồn)
    // 3. Chạy các tác vụ auto, select {case <-ctx.Done(): ...}
    // 4. Đóng conn và trả lỗi đã che dữ liệu nhạy cảm
    return nil
}
func newGameWorker() worker { return &gameAdapter{} }
```

Chỉ cho phép `gameAutomation=true` khi đã có worker login thật và test. Hiện `CapabilitiesJSON()` trả `false` cho từng auto Farm/Fishing/Teacher/Diamond; sau này cần expose theo module đang hoạt động, không đánh đồng với sự tồn tại của generic worker.

## 5. Kiểm thử bằng fixtures trước khi dùng server thật

- Golden byte fixtures (không chứa mật khẩu): handshake, login response, phiên hợp lệ/sai server, timeout/reconnect.
- Packet malformed/fuzz: tuyệt đối không panic/đọc quá bộ đệm.
- Worker: dừng trong vòng timeout khi mất mạng, không goroutine leak, không duplicate login khi resume.
- Proxy: IP/no-auth/user-pass, server từ chối, auth fail, mạng chập chờn.
- Service: Doze, khóa màn hình, Android 13–16, thiếu thông báo, hệ thống kill/reboot; không tự cam kết 24/7.
- Account security: không log mật khẩu/token, không phát qua HTTP local, backup dữ liệu có chủ đích.

## 6. Điểm cần thêm với source Go gốc

Để hoàn thiện chức năng game, cần thư mục chứa `go.mod`, `internal/session/*.go`, `internal/proto/*.go`, `internal/farmer/*.go`, `internal/manager/*.go`, `internal/vault/*.go`, cùng các cấu hình asset. Chỉ file `.exe`/ELF PC không thể thay thế các thành phần này.

## 7. Các module đã viết lại trong v0.2

- `protocol/frame.go`: cấu trúc plain frame suy ra từ PC `EncodeFrame/DecodeFrame`; phải kiểm chứng bytes bằng fixtures game trước khi đưa vào production.
- `protocol/modifiedutf.go`: Java Modified UTF-8 reader/writer, chưa ghép vào lệnh login.
- `gamewire.go`: socket lifecycle + frame send/receive, không tự gửi bất kỳ lệnh game nào. Khi `ctx.Done()` sẽ đóng kết nối để giải phóng read blocking.
- `farmplanner.go`: cho đầu vào JSON snapshot (sơ đồ cây, vật nuôi) để xuất ra chuỗi hành động offline, chưa có parser snapshot từ game hay executor packet.
- `docs/REVERSE_ENGINEERING.md`: bằng chứng protocol từ disassembly và danh sách 493 Go symbols từ PC stripped binary.
- `tools/build_android.sh` / `.ps1`: build tự động với toolchain Android đã cài; `.github/workflows/build-android.yml` để có thể thử build CI.

**Không được tạo `newGameWorker()` giả trả success**: chỉ bật `gameAutomation` sau khi có thao tác game thật, được thử bằng fixtures và bằng tài khoản thử nghiệm được phép. Mật khẩu Android không được đưa vào `Account.Options` hoặc nhật ký; cần thiết kế giao tiếp credential riêng sau khi có authenticated game client.
