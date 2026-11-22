import com.avatarbot.mobile.legacy.LegacyBotRuntime;
import farm2x.Cmd;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** Tests real JAR auth-only path: handshake/LoginFail and LoginSuccess; does not start auto. */
public final class LoginProbeOfflineTest {
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    private static void scenario(boolean success) throws Exception {
        try(ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())) {
            AtomicReference<Throwable> failure=new AtomicReference<>();
            Thread mock=new Thread(()-> {
                try(Socket socket=server.accept()) {
                    socket.setSoTimeout(5000);
                    InputStream in=socket.getInputStream();OutputStream out=socket.getOutputStream();
                    check(in.read()==(Cmd.KeyRequest&255), "missing key request");
                    check(in.read()==0 && in.read()==0, "bad key request length");
                    out.write(new byte[]{Cmd.KeyRequest,0,4,3,10,20,30});out.flush();
                    byte[] key={10,30,0};int pos=0;
                    int[] commands={Cmd.SetHandler,Cmd.SetProvider,Cmd.SetAgent,Cmd.Device,Cmd.Login};
                    for (int expected:commands) {
                        int cmd=(in.read()^key[pos++%key.length])&255;
                        int hi=(in.read()^key[pos++%key.length])&255;
                        int lo=(in.read()^key[pos++%key.length])&255;
                        int size=(hi<<8)|lo;
                        check(cmd==(expected&255),"unexpected command " + cmd);
                        check(size>=0 && size<10000,"bad length");
                        byte[] data=in.readNBytes(size);
                        check(data.length==size,"truncated packet");
                        for(int i=0;i<size;i++)data[i]^=key[pos++%key.length];
                    }
                    byte[] payload=success?new byte[256]:new byte[0];
                    if(success)payload[3]=101;
                    byte[] packed=new byte[3+payload.length];
                    int cmd=success?Cmd.LoginSuccess:Cmd.LoginFail;
                    packed[0]=(byte)(cmd^key[0]);
                    packed[1]=(byte)((payload.length>>8)^key[1]);
                    packed[2]=(byte)(payload.length^key[2]);
                    for(int i=0;i<payload.length;i++)packed[i+3]=(byte)(payload[i]^key[(i+3)%key.length]);
                    out.write(packed);out.flush();
                    Thread.sleep(400);
                }catch(Throwable t){failure.set(t);}
            });
            mock.start();
            String result=LegacyBotRuntime.testLogin("demo_account", "test_secret", "127.0.0.1",server.getLocalPort(),"");
            check(result.equals(success?"success":"rejected"),"Wrong auth probe result: "+result);
            check(LegacyBotRuntime.probeState().equals(success?"success":"rejected"),"Wrong auth state");
            check(!LegacyBotRuntime.active(),"Login-only test left active worker");
            check(!LegacyBotRuntime.probeMessage().contains("test_secret"),"Probe exposed password");
            mock.join(2000);
            check(!mock.isAlive(),"Mock server hung");
            if(failure.get()!=null)throw new AssertionError("Mock server failure",failure.get());
        }
    }
    public static void main(String[] args)throws Exception {
        scenario(false);
        scenario(true);
        System.out.println("PASS: login-only probe checks LoginFail and LoginSuccess without starting automation");
    }
}
