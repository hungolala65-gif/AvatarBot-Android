import com.avatarbot.mobile.legacy.LegacySessionInspector;
import farm2x.Bot;
import farm2x.BotWorker;
import farm2x.Config;
import farm2x.Session;
import java.lang.reflect.Field;

/** Checks the exact original JAR field layout without a game connection. */
public final class LiveSessionInspectorOfflineTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static void field(Object object, Class<?> owner, String name, Object value) throws Exception {
        Field f=owner.getDeclaredField(name); f.setAccessible(true); f.set(object,value);
    }
    public static void main(String[] args) throws Exception {
        check("unavailable".equals(LegacySessionInspector.inspect(null)), "null worker");
        Config cfg=Config.load();
        cfg.username="test_account"; cfg.password="test_password";
        BotWorker worker=new BotWorker(cfg, 0);
        check("awaiting".equals(LegacySessionInspector.inspect(worker)), "worker before creating Bot");
        Bot bot=new Bot(cfg);
        field(worker,BotWorker.class,"bot",bot);
        check("awaiting".equals(LegacySessionInspector.inspect(worker)), "no session yet");
        Session session=new Session();
        field(bot,Bot.class,"session",session);
        check("awaiting".equals(LegacySessionInspector.inspect(worker)), "open but unauthenticated session");
        field(bot,Bot.class,"ownerID",Integer.valueOf(101));
        check("confirmed".equals(LegacySessionInspector.inspect(worker)), "positive owner ID after LoginSuccess");
        check(LegacySessionInspector.authenticatedCycleIdentity(worker) == bot,
            "current Bot is evidence of LoginSuccess");
        session.close();
        check("disconnected".equals(LegacySessionInspector.inspect(worker)), "closed authenticated session");
        check(LegacySessionInspector.authenticatedCycleIdentity(worker) == bot,
            "fast successful cycle must not disappear after socket closes");
        Bot nextCycle=new Bot(cfg);
        field(worker,BotWorker.class,"bot",nextCycle);
        check(LegacySessionInspector.authenticatedCycleIdentity(worker) == null,
            "next cycle must not inherit previous LoginSuccess");
        field(nextCycle,Bot.class,"session",new Session());
        check(LegacySessionInspector.authenticatedCycleIdentity(worker) == null,
            "new unauthenticated session must not inherit previous LoginSuccess");
        field(nextCycle,Bot.class,"ownerID",Integer.valueOf(101));
        check(LegacySessionInspector.authenticatedCycleIdentity(worker) == nextCycle,
            "new LoginSuccess must use a different per-cycle identity");
        System.out.println("PASS: JAR session inspector distinguishes pending/LoginSuccess/disconnected");
    }
}
