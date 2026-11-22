import farm2x.Config;
import farm2x.Cmd;
import farm2x.PacketReader;
import farm2x.PacketWriter;
import farm2x.Payloads;
import farm2x.Session;
import farm2x.Message;
import javax.microedition.io.Connector;
import javax.microedition.rms.RecordStore;
import java.lang.reflect.Method;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** No connection to real game servers; local in-memory / loopback fixtures only. */
public final class LegacyOfflineTest {
    private static int tests=0;
    static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); tests++; }
    public static void main(String[] args) throws Exception {
        Config cfg=Config.load();
        check(cfg.port==19128,"Config default port");
        check(cfg.username.equals(""),"No default username");
        cfg.username="offline_user";cfg.actionDelayMs=123;
        cfg.save();
        Config loaded=Config.load();
        check("offline_user".equals(loaded.username) && loaded.actionDelayMs==123,"RMS Config persistence while app process alive");
        RecordStore.deleteRecordStore("farm2x-config");
        Class<?> teacher=Class.forName("farm2x.TeacherStore");
        Method lookup=teacher.getDeclaredMethod("lookup",int.class,String.class);lookup.setAccessible(true);
        check(lookup.invoke(null,0,"not_registered")==null,"TeacherStore memory RMS initialized");
        Class<?> stats=Class.forName("farm2x.Stats");
        Method kcx=stats.getDeclaredMethod("kcx");kcx.setAccessible(true);
        check(((Number)kcx.invoke(null)).intValue()>=0,"Stats memory RMS initialized");
        PacketWriter p=new PacketWriter().i8(-5).i16(258).i32(0x12345678).utf("Tết");
        PacketReader r=new PacketReader(p.toByteArray());
        check(r.i8()==-5,"byte roundtrip");
        check(r.u16()==258,"short roundtrip");
        check(r.i32()==0x12345678,"int roundtrip");
        check("Tết".equals(r.utf()),"modified UTF-8 roundtrip");
        check(Cmd.FarmPlant==64 && Cmd.KeyRequest==-27,"Opcode constants");
        check(Payloads.login("a","b","2.5.8").length>0,"Real login payload builder accessible");
        testHandshake(0);
        testHandshake(1);
        testHandshake(2);
        System.out.println("PASS " + tests + " offline assertions");
    }
    static void testHandshake(int proxyMode) throws Exception {
        final boolean proxied = proxyMode!=0, auth=proxyMode==2;
        try(ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())) {
            int listenPort=server.getLocalPort();
            AtomicReference<Throwable> thrown=new AtomicReference<>();
            Thread peer=new Thread(()->{
                try(Socket socket=server.accept()) {
                    socket.setSoTimeout(5000);
                    InputStream in=socket.getInputStream();OutputStream out=socket.getOutputStream();
                    if(proxied){
                        check(in.read()==5,"socks5 version");
                        int cnt=in.read(); check(cnt==1,"socks5 method count");
                        check(in.read()==(auth?2:0),"socks5 auth method offered");
                        out.write(new byte[]{5,(byte)(auth?2:0)}); out.flush();
                        if(auth) {
                            check(in.read()==1,"socks5 RFC1929 auth version");
                            int ul=in.read();byte[] u=in.readNBytes(ul);
                            int pl=in.read();byte[] pw=in.readNBytes(pl);
                            check("demo".equals(new String(u)) && "p@ss".equals(new String(pw)),"socks5 credentials correctly parsed");
                            out.write(new byte[]{1,0});out.flush();
                        }
                        check(in.read()==5 && in.read()==1 && in.read()==0 && in.read()==3,"socks5 connect header");
                        int n=in.read();byte[] host=in.readNBytes(n);check("127.0.0.1".equals(new String(host)),"socks5 target host");
                        in.readNBytes(2);
                        out.write(new byte[]{5,0,0,1,127,0,0,1,0,1}); out.flush();
                    }
                    check(in.read()==(Cmd.KeyRequest&255),"legacy key request");
                    check(in.read()==0 && in.read()==0,"legacy request body empty");
                    out.write(new byte[]{Cmd.KeyRequest,0,4,3,10,20,30});out.flush();
                    // Effective XOR key after cumulative transformation: 10,30,0.
                    byte[] key=new byte[]{10,30,0};
                    byte[] message=new byte[]{42,0,3,1,2,3};
                    for(int x=0;x<message.length;x++)message[x]^=key[x%key.length];
                    out.write(message);out.flush();
                    // Verify the original JAR's send() encrypts header and payload too.
                    byte[] actual=in.readNBytes(5);
                    check(actual.length==5,"encrypted outgoing frame read");
                    for(int x=0;x<actual.length;x++)actual[x]^=key[x%key.length];
                    check(Arrays.equals(actual,new byte[]{62,0,2,4,5}),"encrypted outgoing frame decrypted");
                    Thread.sleep(80);
                }catch(Throwable t){thrown.set(t);}
            }); peer.start();
            if(proxied){
                Connector.authorize("127.0.0.1",12345,(auth?"socks5://demo:p%40ss@127.0.0.1:":"socks5://127.0.0.1:")+listenPort);
            } else {
                Connector.authorize("127.0.0.1",listenPort,"");
            }
            boolean blocked=false;
            try { Connector.open("socket://example.invalid:9999"); }
            catch(IOException expected){ blocked=true; }
            check(blocked,"unknown external host is blocked");
            Session game=new Session();
            try { game.connect("127.0.0.1",proxied?12345:listenPort);
                check(!game.isClosed(),"JAR handshake accepted fixture");
                Message m=game.nextFrame(1500);
                check(m!=null && m.command==42 && Arrays.equals(m.data,new byte[]{1,2,3}),"Encrypted incoming frame decoded");
                game.send((byte)62,new byte[]{4,5});
            } finally { game.close(); Connector.revoke(); }
            peer.join(3000);
            if(thrown.get()!=null)throw new AssertionError("Peer failed",thrown.get());
        }
    }
}
