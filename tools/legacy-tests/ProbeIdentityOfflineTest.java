import com.avatarbot.mobile.legacy.LegacyBotRuntime;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public final class ProbeIdentityOfflineTest {
    private static void ok(boolean b, String msg) { if (!b) throw new AssertionError(msg); }
    public static void main(String[] args) throws Exception {
        try(ServerSocket server=new ServerSocket(0)) {
            int port=server.getLocalPort();
            // An empty mock server is enough to inspect identity set at probe start.
            Thread listener=new Thread(()->{
                try(Socket s=server.accept()) { Thread.sleep(250); }
                catch(Exception e) { }
            }); listener.start();
            Thread probe=new Thread(()->LegacyBotRuntime.testLogin("test-user", "test-secret", "127.0.0.1",port,""));
            probe.start();
            long deadline=System.currentTimeMillis()+3000;
            while(!LegacyBotRuntime.probeMatches("test-user","test-secret","127.0.0.1:"+port) &&
                System.currentTimeMillis()<deadline) Thread.sleep(10);
            ok(LegacyBotRuntime.probeMatches("test-user","test-secret","127.0.0.1:"+port),"matching credential identity");
            ok(!LegacyBotRuntime.probeMatches("test-user","wrong","127.0.0.1:"+port),"wrong password must not show prior success");
            ok(!LegacyBotRuntime.probeMatches("another-user","test-secret","127.0.0.1:"+port),"other user must not reuse probe");
            ok(!LegacyBotRuntime.probeMatches("test-user","test-secret","localhost:"+port),"other endpoint must not reuse probe");
            LegacyBotRuntime.stop();
            probe.join(2000);
            listener.join(2000);
            ok("stopped".equals(LegacyBotRuntime.state())||"failed".equals(LegacyBotRuntime.state()), "stop must cancel session");
        }
        LegacyBotRuntime.markNetworkWaiting(true);
        ok(LegacyBotRuntime.reserveStartTicket() == -1,
            "A pending offline session must block concurrent login/worker reservation");
        ok("network_wait".equals(LegacyBotRuntime.state()), "UI knows network wait state");
        LegacyBotRuntime.stop();
        ok(!"network_wait".equals(LegacyBotRuntime.state()), "stop clears network wait state");
        System.out.println("PASS: probe isolated per credentials/server; network state cancel");
    }
}
