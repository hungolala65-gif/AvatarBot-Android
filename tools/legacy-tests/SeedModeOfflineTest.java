import com.avatarbot.mobile.legacy.LegacyBotRuntime;
import farm2x.Config;
import java.lang.reflect.*;

public final class SeedModeOfflineTest {
    public static void main(String[] args) throws Exception {
        Method m = LegacyBotRuntime.class.getDeclaredMethod("createConfig", String.class,
            String.class,String.class,int.class,int.class,boolean.class,boolean.class,
            int.class,int.class,String.class);
        m.setAccessible(true);
        for(String selected: new String[]{"preferred", "harvest_replay", "off"}) {
            Config cfg=(Config)m.invoke(null,"user","secret","127.0.0.1",19128,0,
                false,false,0,15,selected);
            if(!selected.equals(cfg.seedMode))
                throw new AssertionError("Seed mode not mapped: "+selected);
        }
        for(String invalid:new String[]{"", "AUTO", "unexpected"}) {
            try {
                m.invoke(null,"user","secret","127.0.0.1",19128,0,
                    false,false,0,15,invalid);
                throw new AssertionError("Unsupported seed mode accepted");
            } catch(InvocationTargetException e) {
                if(!(e.getCause() instanceof IllegalArgumentException)) throw e;
            }
        }
        System.out.println("PASS: seed modes preferred, harvest_replay and off mapped to JAR");
    }
}
