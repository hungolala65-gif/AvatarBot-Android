import com.avatarbot.mobile.legacy.LegacyBotRuntime;
import farm2x.Bot;
import farm2x.BotWorker;
import farm2x.Config;
import farm2x.Session;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The JAR creates a fresh Bot per worker cycle. Ensure that short-lived
 * LoginSuccess events are counted only once, even after the socket closes.
 * No server, network or game credentials are used.
 */
public final class LoginCycleEvidenceOfflineTest {
    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
    static void field(Object o, Class<?> cls, String name, Object value) throws Exception {
        Field f=cls.getDeclaredField(name); f.setAccessible(true); f.set(o,value);
    }
    public static void main(String[] args) throws Exception {
        Config cfg=Config.load(); cfg.username="dummy"; cfg.password="dummy";
        BotWorker worker=new BotWorker(cfg,0);
        CountDownLatch release=new CountDownLatch(1);
        Thread fakeRunningWorker=new Thread(() -> {
            try { release.await(4,TimeUnit.SECONDS); }
            catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }, "test-only-worker-alive");
        fakeRunningWorker.start();
        field(null,LegacyBotRuntime.class,"worker",worker);
        field(null,LegacyBotRuntime.class,"thread",fakeRunningWorker);
        field(null,LegacyBotRuntime.class,"lastAuthenticatedBotIdentity",null);
        field(null,LegacyBotRuntime.class,"observedLoginCycles",Long.valueOf(0));
        try {
            check(LegacyBotRuntime.observedLoginCycles()==0L,"initial evidence empty");
            Bot one=new Bot(cfg);
            field(worker,BotWorker.class,"bot",one);
            field(one,Bot.class,"session",new Session());
            check(LegacyBotRuntime.observedLoginCycles()==0L,"not authenticated yet");
            field(one,Bot.class,"ownerID",Integer.valueOf(37));
            check(LegacyBotRuntime.observedLoginCycles()==1L,"first LoginSuccess counted");
            check(LegacyBotRuntime.observedLoginCycles()==1L,"same cycle not double-counted");
            Session old=(Session) getField(one,Bot.class,"session");
            old.close();
            check(LegacyBotRuntime.observedLoginCycles()==1L,"closed socket cannot erase evidence");
            Bot two=new Bot(cfg);
            field(worker,BotWorker.class,"bot",two);
            field(two,Bot.class,"session",new Session());
            check(LegacyBotRuntime.observedLoginCycles()==1L,"new cycle awaiting must not increment");
            field(two,Bot.class,"ownerID",Integer.valueOf(37));
            check(LegacyBotRuntime.observedLoginCycles()==2L,"second cycle with same ID counts separately");
            check(LegacyBotRuntime.observedLoginCycles()==2L,"no double-count on second cycle");
        } finally {
            release.countDown(); fakeRunningWorker.join(4000);
            field(null,LegacyBotRuntime.class,"thread",null);
            field(null,LegacyBotRuntime.class,"worker",null);
        }
        System.out.println("PASS: LoginSuccess evidence counted once per JAR cycle, even after socket closes");
    }
    private static Object getField(Object o, Class<?> cls, String name) throws Exception {
        Field f=cls.getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }
}
