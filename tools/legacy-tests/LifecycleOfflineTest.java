import com.avatarbot.mobile.legacy.LegacyBotRuntime;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** A stop must unblock an active network read, not just forbid future TCP opens. */
public final class LifecycleOfflineTest {
    public static void main(String[] args) throws Exception {
        if (!LegacyBotRuntime.reserveStart()) throw new AssertionError("First reservation denied");
        if (LegacyBotRuntime.reserveStart()) throw new AssertionError("Duplicate reservation accepted");
        if (!"starting".equals(LegacyBotRuntime.state())) throw new AssertionError("Missing STARTING state");
        LegacyBotRuntime.stop();
        if (LegacyBotRuntime.active()) throw new AssertionError("STOP must cancel pending start");
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            CountDownLatch accepted = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            Thread remote = new Thread(() -> {
                try (Socket client = server.accept()) {
                    accepted.countDown();
                    release.await(2, TimeUnit.SECONDS);
                } catch (Exception ignored) { }
            });
            remote.start();
            Connector.authorize("127.0.0.1", server.getLocalPort(), "");
            SocketConnection conn = (SocketConnection) Connector.open("socket://127.0.0.1:" + server.getLocalPort());
            if (!accepted.await(2, TimeUnit.SECONDS)) throw new AssertionError("No accepted socket");
            CountDownLatch reading = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread reader = new Thread(() -> {
                try {
                    reading.countDown();
                    int value = conn.openDataInputStream().read();
                    if (value != -1) throw new AssertionError("Stopped connection returned data");
                } catch (IOException expected) {
                    // Expected when stop closes a socket while its read() is blocked.
                } catch (Throwable unexpected) { error.set(unexpected); }
            });
            reader.start();
            if (!reading.await(1, TimeUnit.SECONDS)) throw new AssertionError("Reader never started");
            Connector.revoke();
            reader.join(1500);
            if (reader.isAlive()) throw new AssertionError("Read not interrupted by revoke");
            if (error.get() != null) throw new AssertionError("Unexpected reader error", error.get());
            boolean denied = false;
            try { Connector.open("socket://127.0.0.1:" + server.getLocalPort()); }
            catch (IOException expected) { denied = true; }
            if (!denied) throw new AssertionError("Connection allowed after revoke");
            release.countDown();
            remote.join(2000);
        }
        System.out.println("PASS: start reservation, stop cancellation, live socket close and connect denial");
    }
}
