import javax.microedition.io.Connector;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** A silent SOCKS5 proxy must not trap a stopped background worker. */
public final class SocksHandshakeCancellationTest {
    public static void main(String[] args) throws Exception {
        try (ServerSocket proxy = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            final CountDownLatch accepted = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            Thread server = new Thread(() -> {
                try (Socket client = proxy.accept()) {
                    accepted.countDown();
                    release.await(5, TimeUnit.SECONDS);
                } catch (Exception ignored) { }
            }, "offline-silent-proxy");
            server.start();
            Connector.authorize("127.0.0.1", 19128,
                "socks5://127.0.0.1:" + proxy.getLocalPort());
            AtomicReference<Throwable> result = new AtomicReference<>();
            Thread client = new Thread(() -> {
                try {
                    Connector.open("socket://127.0.0.1:19128");
                    result.set(new AssertionError("SOCKS connection unexpectedly succeeded"));
                } catch (IOException expected) { }
                catch (Throwable e) { result.set(e); }
            }, "offline-proxy-client");
            client.start();
            if (!accepted.await(2, TimeUnit.SECONDS))
                throw new AssertionError("Proxy was not connected");
            Connector.revoke();
            client.join(2000);
            release.countDown();
            server.join(2000);
            if (client.isAlive()) throw new AssertionError("Stop did not interrupt silent SOCKS handshake");
            if (result.get() != null) throw new AssertionError("Unexpected SOCKS result", result.get());
            System.out.println("PASS: Stop interrupts blocked SOCKS5 authentication handshake");
        }
    }
}
