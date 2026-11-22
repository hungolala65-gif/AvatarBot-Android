# AvatarBot Android Native v0.17 — source-only handoff

**Build status:** Offline Java ME and Go tests passed; Kotlin source parsed. APK v0.17 was **not** built, because this execution environment has no Android SDK / Gradle and the linked GitHub API returns `403` (account suspended). The last pre-existing installable APK was v0.10, which DOES NOT contain v0.17 changes. Do not rename older APKs.

## What v0.17 actually improves

- Detects repeated lack of `LoginSuccess` even when the original Java ME `BotWorker` continues running. A 3-minute initial validation guard and 10-minute post-login grace stop falsely idle workers; no automatic password retries.
- Observes a **real LoginSuccess** for each new `farm2x.Bot` cycle, including when the socket closes before the 1.5-second Android monitor polls it. This evidence is not a harvest, fish, teacher answer or diamond sale.
- Encrypted account-scoped journal records initial login, reconfirmed login, and authentication-stalled stop; status UI distinguishes worker, active game socket, and prior login evidence.
- Includes a CI workflow and `tools/build_v017_apk.sh` that will build and verify a **genuine** APK once SDK and an authorized CI account are available.

## Build and validation

Run `bash tools/verify_legacy.sh`, `bash tools/verify_setup_options_v15.sh`, and `bash tools/verify_offline.sh` for source-level tests where the required toolchains are available. With Android SDK Platform 35, Android Build Tools 35.0.0 and Gradle 8.9 available, run `bash tools/build_v017_apk.sh` to create `dist/AvatarBot-Android-v0.17-debug.apk` and checksum.

**Limits:** no confirmed real-server login or game action; no multi-account concurrent execution; Foreground Service does not guarantee 24/7 operation across Android manufacturers. Do not use a valuable account for unverified Auto; respect the game's terms. The included APK signing path is a development debug build, not an app-store release.

See `docs/CHANGELOG_V017.md`, `docs/TEST_RESULTS_V017.md`, `docs/BUILD_V017.md` and `docs/PC_ANDROID_PARITY_V017.md`.

---

## Earlier v0.16 documentation (historical)

# AvatarBot Android Native v0.16 — Java ME live-session status

**Tình trạng:** Mã nguồn thử nghiệm, đã kiểm thử offline; **chưa build APK v0.16** và **chưa xác minh game thật**. Bản PC chỉ có nhị phân amd64, bản Android tích hợp Java ME Core; hiện chỉ một worker tại một thời điểm.

**Mới trong v0.16:** phân biệt worker còn chạy với phiên game đã nhận `LoginSuccess` trên socket đang mở. Xem `docs/RESEARCH_AND_CHANGES_V016.md`.

**Tự build APK (máy có Android SDK/Gradle):** mở thư mục `android` trong Android Studio hoặc chạy workflow `.github/workflows/build-apk.yml` trên repository riêng tư có quyền GitHub Actions. Giữ nguyên thư mục `android/app/libs/` và mã Kotlin/Java đầy đủ. Không sử dụng file APK v0.10 để thay cho bản v0.16.

---

# AvatarBot Android Native v0.15 — Setup chuyên sâu theo từng Auto

**Mục tiêu bản này:** Có 4 trang Setup độc lập cho Farm/Fishing/Teacher/Diamond, giữ cấu hình riêng cho từng tài khoản trong Android Keystore AES-GCM, và nhật ký **sự kiện thực thi có thật** theo tài khoản. Bản này là **mã nguồn thử nghiệm**, không phải APK đã xác nhận hoặc bản clone PC 1:1.

- Vào **Tài khoản → Thiết lập**, hoặc **Auto → Thiết lập** trên tài khoản cần chỉnh, để mở trang Setup chuyên sâu.
- Từng trang có công tắc mode, thông số gốc xác nhận qua `farm2x.Config`, phần thiết lập treo, và nút **Lưu Setup**. Các mã vật phẩm/bản đồ từ JAR chưa xác minh với game thật; hãy giữ mặc định nếu chưa biết mã chính xác.
- Sau khi lưu, phải **Dừng → Chạy** để cấu hình mới có hiệu lực. Một tài khoản/mode mỗi lần do JAR giữ trạng thái toàn cục.
- Nhấn **Thử đăng nhập** để kiểm tra LoginSuccess mà không chạy Auto. Trạng thái "worker bắt đầu" **không** có nghĩa login thành công, thu hoạch thành công hay kiếm kim cương.
- Nhật ký sự kiện (`SecureExecutionJournal`) mã hóa bằng Keystore, lưu tối đa 150 sự kiện, bao gồm tài khoản UUID/mode/giai đoạn/thời điểm **không có username/password/proxy**. Nhật ký JAR hiện tại chỉ hiển thị riêng khi tài khoản đang chạy và có lọc dữ liệu nhạy cảm.
- Công tắc bán vật nuôi và KCX/NHB mặc định tắt, JAR bị khóa hành vi tiêu Lượng, mua vật nuôi, tự bán cả kho; không bật tự động bằng thay đổi Setup.

**Không xác minh được**: đăng nhập server thực, farm/fishing/teacher/diamond hoàn tất lệnh trong game, chạy nhiều tài khoản cùng lúc, treo ổn định 8–24 giờ trên thiết bị, quyền phân phối bản JAR. Không nên dùng tài khoản chính trước khi thử nghiệm.

Hướng dẫn kiểm thử và build: `docs/BUILD_V015.md`. Bảng phạm vi: `docs/CHANGELOG_V015.md`.

# AvatarBot Android Native v0.14 — Session safety and strict server selection

v0.14 fixes duplicate Start commands mutating an active worker session and prevents corrupted city codes from silently routing account credentials to Hoàn Mỹ. See `docs/CHANGELOG_V014.md` and `docs/BUILD_V014.md`. **All available offline checks passed; this is source code, not a verified Android APK or confirmed live-game bot.**

---

# AvatarBot Android Native v0.13 — Setup safety and JAR parity

Bản v0.13 sửa cấu hình Auto để các thiết lập hiển thị **đúng các trường JAR đã kiểm chứng**. Sửa lỗi mặc định tự bán KCX/NHB và bán vật nuôi, bổ sung tùy chọn opt-in, số vòng câu cá và khu vực giáo viên. Xem `docs/CHANGELOG_V013.md` và `docs/BUILD_V013.md`. **Chưa có kết quả đăng nhập server thật, kiểm thử Android, hoặc APK v0.13.**

---

# AvatarBot 2X Android Native v0.12 (experimental)

**Mục tiêu:** chạy một phiên bot Java ME trên Android bằng APK Kotlin/Jetpack Compose và Android Foreground Service, không cần PC/VPS. Đây là bản **mã nguồn thử nghiệm**, không phải bản phát hành ổn định hay bản clone 1:1 công cụ PC.

## Trạng thái có thể xác minh

- Java ME `bot2x-headless.jar` xuất phát từ file `Bot2X-SuperLite.jar` do người dùng cung cấp. Các chế độ Auto Farm, Fishing, Diamond và Teacher có logic JAR, nhưng **chưa được chứng minh hoạt động trên server game thật**.
- Thử đăng nhập chỉ kiểm thử được với server mô phỏng, không có kết quả LoginSuccess thật.
- App chỉ cho chạy **một tài khoản đồng thời**, khác với bản PC có nhiều tài khoản/Proxy/VPS node.
- APK v0.10 **đã build thành công** trong các lượt CI trước đó. **v0.12 chưa build APK được trong phiên này**, do thiếu Android SDK/Gradle tại chỗ và GitHub từ chối API với thông báo tài khoản bị đình chỉ (403). Không đổi tên ZIP thành APK.
- Cấu trúc Android v0.12 đã qua kiểm thử Java/JVM, Go, Kotlin CLI và parser PSI; đó **không** thay thế Gradle build hoặc thử trên điện thoại thật.

## Cập nhật v0.12

1. **Chờ kết nối mạng có giới hạn:** khi worker thực sự kết thúc và người dùng đã bật tùy chọn thử lại, nếu thiết bị mất mạng thì giữ trạng thái `network_wait` trong tối đa 30 phút; không tiêu thêm lượt khởi chạy lại trong lúc chờ. Dựa trên ConnectivityManager; khi mạng có lại, dùng lịch thử lại 30/60/120 giây. Bấm Dừng luôn hủy cả thời gian chờ mạng. Chỉ kiểm thử offline, chưa kiểm thử các tình huống Android giết tiến trình.
2. **Kết quả kiểm tra đăng nhập theo đúng tài khoản/server:** kết quả cũ không được hiển thị cho tài khoản, mật khẩu hoặc thành phố khác; thao tác Chạy Auto sẽ xóa trạng thái của lần probe trước.
3. **Lựa chọn gieo hạt:** `preferred` (JAR mặc định), `harvest_replay` (gieo lại giống vừa thu), hoặc không gieo (`off`). Khi tắt gieo, bot có thể vẫn thu hoạch và để đất trống; cần cân nhắc trước khi sử dụng.
4. **Sao chép chẩn đoán:** tab Logs có nút sao chép nhật ký, dùng thuật toán che mật khẩu, tên tài khoản, token và SOCKS5 theo mẫu. Phải kiểm tra nội dung thủ công trước khi chia sẻ.
5. Ngăn yêu cầu khởi chạy trùng khi đang chờ hồi mạng; sửa đua tranh giữa ticket khởi chạy và WakeLock.

## Cấu trúc dự án

- `android/`: ứng dụng Compose, Foreground Service, Android Keystore, Java ME adapters.
- `android/app/libs/bot2x-headless.jar`: bytecode bot đã trích từ JAR gốc; không chứa tài khoản người dùng.
- `go-mobilecore/`: thành phần Go thử nghiệm, **không** phải game bot đang điều khiển app.
- `tools/`: kiểm thử offline Kotlin/Java/Go và kết nối mock, không gửi lưu lượng tới server game thật.
- `.github/workflows/build-apk.yml`: quy trình build APK bằng Android SDK 35 và Gradle 8.9 trên GitHub Actions khi repo hoạt động.
- `docs/CHANGELOG_V012.md`: thay đổi và giới hạn chính xác.
- `docs/BUILD_V012.md`: hướng dẫn build và thử nghiệm trên điện thoại.

## Chạy kiểm thử

Từ thư mục gốc (cần Java 17+, Go và Kotlin CLI):

```bash
bash tools/verify_offline.sh
```

Để build APK (cần Android SDK 35 / JDK 17 / Gradle 8.9):

```bash
cd android
gradle wrapper --gradle-version 8.9
./gradlew --no-daemon assembleDebug
```

Lấy kết quả ở `android/app/build/outputs/apk/debug/app-debug.apk` khi và chỉ khi lệnh build thành công. Vui lòng đọc `docs/BUILD_V012.md` trước khi thử đăng nhập/Auto.

**Không nhập tài khoản thật hoặc đăng tải mật khẩu lên GitHub; tránh sử dụng tài khoản quan trọng khi chưa xác minh giao thức và bảo mật.**
