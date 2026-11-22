import farm2x.Config;
import com.avatarbot.mobile.legacy.LegacyBotRuntime;
import java.lang.reflect.*;

public class LoopIntervalOfflineTest {
    public static void main(String[] args) throws Exception {
        Method m = LegacyBotRuntime.class.getDeclaredMethod("createConfig", String.class,
            String.class, String.class, int.class, int.class, boolean.class, boolean.class,
            int.class, int.class);
        m.setAccessible(true);
        for (int interval : new int[]{15,30,60,120,300}) {
            Config cfg=(Config)m.invoke(null,"dummy","dummy","127.0.0.1",19128,0,
                false,false,0,interval);
            if (cfg.loopDelayMs != interval * 1000) throw new AssertionError("Delay mismatch "+interval);
        }
        for (int invalid : new int[]{-1,0,1,14,301,10000}) {
            try {
                m.invoke(null,"dummy","dummy","127.0.0.1",19128,0,false,false,0,invalid);
                throw new AssertionError("Invalid interval was accepted: "+invalid);
            } catch (InvocationTargetException ex) {
                if (!(ex.getCause() instanceof IllegalArgumentException)) throw ex;
            }
        }
        System.out.println("PASS: configurable cycle interval (JAR Config.loopDelayMs)");
    }
}
