# Khảo sát tĩnh giao thức từ bản PC (không dùng tài khoản thật)

Bản `avatar-bot-linux-amd64` trong `AvatarBot-Fix-Full.zip` là ELF x86-64, static Go 1.21.13, `trimpath=true`, `CGO_ENABLED=0`, `stripped`. Go buildinfo có `vcs.revision=a0dc60548552b972e6836ab94fdb45c471e92967`; **không** chứa địa chỉ repo hay Go source.

Dù ELF đã strip bảng symbol, `.gopclntab` vẫn còn. Dùng `debug/gosym` đọc được **493 tên hàm/ký hiệu thuộc `avatar-bot/internal/`**. Danh sách đầy đủ ở [`INTERNAL_SYMBOLS_PC.txt`](INTERNAL_SYMBOLS_PC.txt). Có thể tái tạo bằng `go run tools/extract_pc_go_symbols.go /path/to/avatar-bot-linux-amd64`.

## Kết quả chắc chắn và mức độ xác minh

- Có `internal/proto.EncodeFrame` tại `0x693e20`, `internal/proto.DecodeFrame` tại `0x693fe0`, `internal/proto.readFull`, `Reader.ReadUTF`, `Writer.WriteUTF`, `encodeModifiedUTF`, `decodeModifiedUTF`.
- Phân tích opcode assembly của `EncodeFrame` xác định vùng đệm dài `bodyLength + 3`, đặt 1 byte đầu là loại packet và hai byte tiếp là độ dài `uint16` thứ tự big-endian. Dữ liệu body được copy vào sau header.
- `DecodeFrame` đọc 3 byte header, lấy 16-bit độ dài theo big-endian, kiểm giới hạn rồi đọc body. Có vòng XOR theo khóa và vị trí ở cả đầu vào/đầu ra. **Chưa có key trao đổi từ handshake, vì vậy chưa thể dùng cipher với server thật**.
- Có hàm `internal/session.Dial`, `(*Session).handshake`, `Frames`, `Send`, `cipherSnapshot`, `Close`.
- Có hàm `internal/farmer.(*Client).login`, `Run`, `RunFishing`, `RunTeacher`, `RunDiamond`, `ParseLoginSuccess`, cùng hàng trăm handler trạng thái, hành động.

## Mã đã viết lại theo bằng chứng

- `go-mobilecore/protocol/frame.go`: bộ codec type+length+payload và XOR trạng thái độc lập chiều đọc/ghi; kiểm thử roundtrip và malformed frames. **Giả định XOR/key từ phân tích tĩnh phải được kiểm chứng bằng dữ liệu test đã ẩn thông tin nhạy cảm**.
- `go-mobilecore/protocol/modifiedutf.go`: Java Modified UTF-8 với header 2 byte (chuẩn ứng dụng Java); test chuỗi Unicode, ký tự NUL, supplementary codepoint, dữ liệu lỗi. Việc app PC dùng cách xử lý UTF này được củng cố bởi tên hàm, nhưng chưa so byte-for-byte với bản gốc.
- `go-mobilecore/gamewire.go`: kết nối TCP/SOCKS5, read/write frame, cancel qua context; chưa gọi login hay gửi packet tự động.
- `go-mobilecore/farmplanner.go`: offline planner tự viết theo *loại nhiệm vụ* liệt kê trong hướng dẫn PC; **không** khẳng định thuật toán quyết định giống bot PC.

## Chưa thể biết từ tài liệu/nhị phân với độ tin cậy cần thiết

Thứ tự handshake; thỏa thuận khóa mã hóa; opcode và cấu trúc payload của login và các thao tác; xử lý captcha; xác thực session, reconnect theo phản hồi server; định dạng vault PC. Số lượng hàm / disassembly **không tương đương** mã nguồn gốc có thể bảo trì. Chưa có fixture packet thực tế, không có xác nhận tương thích server game.

Không dùng ứng dụng này để gửi thử gói tin không biết ý nghĩa tới server hoặc đăng nhập bằng tài khoản thật khi lớp session chưa có kiểm thử end-to-end.
