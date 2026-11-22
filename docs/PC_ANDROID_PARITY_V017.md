# PC ↔ Android/JAR parity evidence (v0.17)

The PC ZIP `AvatarBot-Fix-Full.zip` actually contains `HUONG-DAN-SU-DUNG.txt`, a Windows amd64 executable and a Linux amd64 executable; it has **zero `.go` source files**. The guide explicitly describes crop harvest/replant, watering/weeding/pest care/fertilizer, livestock purchase/feeding/treatment/product collection/selling, merchant assistance, cooking, star fruit, simultaneous multi-account work, and proxy/VPS use.

The original `Bot2X-SuperLite.jar` contains 60 compiled `farm2x/*.class` entries, of which the 50 headless core classes are byte-for-byte identical in the Android `bot2x-headless.jar` (verified programmatically). The Android adapter maps Auto Farm, Fishing, Teacher and Diamond into JAR `Config`/`BotWorker`, but Android cannot reproduce all Go PC behavior just by wrapping the JAR.

| Area | Supported observation | Still missing |
| --- | --- | --- |
| Login | Java ME handshake and LoginSuccess/Fail tested using mock TCP fixtures | Actual game-server login evidence on a device |
| Farm | Crop/animal/merchant/cooking JAR code and account setup | Independent control of all PC tasks, real-server result verification |
| Fishing | JAR mode and code/area/round settings | Validate actual map/item IDs and successful fishing |
| Teacher | JAR mode and area setting | Real answer success; optional CAPTCHA service deliberately blocked |
| Diamond | JAR mode, opt-in selling controls, time/interval settings | Real sales confirmed by server, account-level stats |
| Continuous operation | Android Foreground Service, optional partial WakeLock, finite retry/network wait | Android device and battery manufacturer tests over 8–24 hours |
| Multiple accounts | Encrypted per-account settings | More than one concurrent worker (original JAR static state prevents safe sharing) |
| Networking | TCP / SOCKS5 offline fixtures | Current live endpoint reachability and protocol compatibility |

No claims of live success or 24/7 availability can be made from these source and simulator tests alone.
