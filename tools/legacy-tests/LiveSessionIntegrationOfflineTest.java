import com.avatarbot.mobile.legacy.LegacySessionInspector;
import farm2x.Bot;
import farm2x.BotWorker;
import farm2x.Config;
import farm2x.Cmd;
import javax.microedition.io.Connector;
import java.net.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Uses original JAR handshake and LoginSuccess on a loopback mock server. */
public final class LiveSessionIntegrationOfflineTest {
    private static void require(boolean condition, String message) {
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        try (ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())) {
            Config config=Config.load(); config.host="127.0.0.1"; config.port=server.getLocalPort();
            config.username="fixture"; config.password="fictional_password";
            AtomicReference<Throwable> error=new AtomicReference<>();
            CountDownLatch releaseServer=new CountDownLatch(1);
            Thread mock=new Thread(() -> {
                try(Socket client=server.accept()) {
                    client.setSoTimeout(4500);
                    InputStream in=client.getInputStream();OutputStream out=client.getOutputStream();
                    require(in.read()==(Cmd.KeyRequest&255) && in.read()==0 && in.read()==0,"KeyRequest");
                    out.write(new byte[]{Cmd.KeyRequest,0,4,3,10,20,30});out.flush();
                    byte[] key={10,30,0};int pos=0;
                    int[] cmds={Cmd.SetHandler,Cmd.SetProvider,Cmd.SetAgent,Cmd.Device,Cmd.Login};
                    for(int expected:cmds){
                        int cmd=(in.read()^key[pos++%3])&255;
                        int hi=(in.read()^key[pos++%3])&255;
                        int lo=(in.read()^key[pos++%3])&255;
                        int len=(hi<<8)|lo;
                        require(cmd==(expected&255) && len>=0 && len<10000,"Unexpected frame");
                        byte[] payload=in.readNBytes(len); require(payload.length==len,"short frame");
                        pos+=len;
                    }
                    byte[] payload=new byte[256];payload[3]=101;
                    byte[] packet=new byte[3+payload.length];
                    packet[0]=(byte)(Cmd.LoginSuccess^key[0]);
                    packet[1]=(byte)(1^key[1]);packet[2]=(byte)(0^key[2]);
                    for(int i=0;i<payload.length;i++)packet[i+3]=(byte)(payload[i]^key[(i+3)%3]);
                    out.write(packet);out.flush();
                    releaseServer.await(3,TimeUnit.SECONDS);
                } catch(Throwable t) {error.set(t);}
            },"mock-LoginSuccess");
            mock.start();
            Connector.authorize(config.host,config.port,"");
            Bot bot=new Bot(config);
            BotWorker worker=new BotWorker(config,0);
            Field bf=BotWorker.class.getDeclaredField("bot");bf.setAccessible(true);bf.set(worker,bot);
            try {
                Method login=Bot.class.getDeclaredMethod("connectLogin",boolean.class);login.setAccessible(true);
                require(Boolean.TRUE.equals(login.invoke(bot,false)),"JAR did not accept LoginSuccess");
                require("confirmed".equals(LegacySessionInspector.inspect(worker)),
                    "JAR inspector did not observe LoginSuccess on active session");
                require(LegacySessionInspector.authenticatedCycleIdentity(worker)==bot,
                    "JAR did not expose successful cycle evidence");
                bot.stop();
                require("disconnected".equals(LegacySessionInspector.inspect(worker)),
                    "Session should show disconnected after stop");
            } finally {
                bot.stop();Connector.revoke();releaseServer.countDown();mock.join(3500);
            }
            require(!mock.isAlive(),"mock thread still running");
            if(error.get()!=null)throw new AssertionError("server failed",error.get());
            System.out.println("PASS: original JAR LoginSuccess on mock TCP -> confirmed -> disconnected");
        }
    }
}
