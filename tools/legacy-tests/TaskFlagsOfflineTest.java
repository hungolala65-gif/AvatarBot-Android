import farm2x.Config;
import com.avatarbot.mobile.legacy.LegacyBotRuntime;
import java.lang.reflect.Method;
public class TaskFlagsOfflineTest {
    public static void main(String[] args) throws Exception {
        Method mapper = LegacyBotRuntime.class.getDeclaredMethod("createConfig",
            String.class, String.class, String.class, int.class, int.class,
            boolean.class, boolean.class, int.class);
        mapper.setAccessible(true);
        for (int mask=0; mask<32; mask++) {
            Config c=(Config) mapper.invoke(null,"user","password","127.0.0.1",19128,0,false,false,mask);
            if (c.autoDailyQuest!=((mask&1)!=0) || c.autoQuestHugo!=((mask&2)!=0)
                || c.autoQuestEvent!=((mask&4)!=0) || c.autoFarmMission!=((mask&8)!=0)
                || c.autoFarmCheckin!=((mask&16)!=0)) throw new AssertionError("feature mask="+mask);
        }
        System.out.println("PASS: 32 Java ME task-flag combinations");
    }
}
