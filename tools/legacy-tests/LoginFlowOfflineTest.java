import farm2x.Bot;
import farm2x.Config;
import farm2x.Cmd;
import farm2x.Payloads;
import javax.microedition.io.Connector;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the actual original JAR login pipeline against local fake server. */
public final class LoginFlowOfflineTest {
  public static void main(String[] args) throws Exception {
    try(ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())) {
      int port=server.getLocalPort();
      Config cfg=Config.load();
      cfg.host="127.0.0.1";cfg.port=port;
      cfg.username="offline_dummy";cfg.password="offline_password";
      AtomicReference<Throwable> failure=new AtomicReference<>();
      Thread mock=new Thread(()->{
        try(Socket client=server.accept()) {
          client.setSoTimeout(4500);
          InputStream in=client.getInputStream(); OutputStream out=client.getOutputStream();
          if(in.read()!= (Cmd.KeyRequest&255) || in.read()!=0 || in.read()!=0) throw new AssertionError("KeyRequest");
          out.write(new byte[]{Cmd.KeyRequest,0,4,3,10,20,30});out.flush();
          byte[] key={10,30,0}; int pos=0;
          int[] cmds={Cmd.SetHandler, Cmd.SetProvider,Cmd.SetAgent, Cmd.Device, Cmd.Login};
          for(int expected:cmds) {
            int command=(in.read()^key[(pos++)%key.length])&255;
            int high=(in.read()^key[(pos++)%key.length])&255;
            int low=(in.read()^key[(pos++)%key.length])&255;
            int len=high*256+low;
            if(command!=(expected&255)) throw new AssertionError("Unexpected opcode: "+command+" expected "+(expected&255));
            if(len<0 || len>10000) throw new AssertionError("Bad frame size");
            byte[] payload=in.readNBytes(len);
            if(payload.length!=len) throw new AssertionError("Truncated packet");
            for(int i=0;i<len;i++) payload[i]^=key[(pos++)%key.length];
            if(expected==Cmd.Login && !Arrays.equals(payload,Payloads.login("offline_dummy","offline_password",cfg.version))) {
              throw new AssertionError("Login payload differs from JAR builder");
            }
          }
          byte[] fail={(byte)(Cmd.LoginFail^key[0]),(byte)(0^key[1]),(byte)(0^key[2])};
          out.write(fail);out.flush();
          Thread.sleep(150);
        }catch(Throwable t){failure.set(t);}
      });
      Connector.authorize(cfg.host,cfg.port,""); mock.start();
      Bot bot=new Bot(cfg);
      try {
        Method login=Bot.class.getDeclaredMethod("connectLogin", boolean.class);
        login.setAccessible(true);
        Object ok=login.invoke(bot, false);
        if(!Boolean.FALSE.equals(ok))throw new AssertionError("Expected fake login failure");
      } finally { bot.stop();Connector.revoke(); }
      mock.join(5000);
      if(mock.isAlive())throw new AssertionError("Mock server didn't finish");
      if(failure.get()!=null)throw new AssertionError("Mock server failed",failure.get());
      System.out.println("PASS: JAR bootstrap (4 packet commands), login payload & LoginFail response on mock server");
    }
  }
}
