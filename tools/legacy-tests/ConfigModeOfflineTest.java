import com.avatarbot.mobile.legacy.LegacyBotRuntime;
import farm2x.Config;
import java.lang.reflect.Method;

/** Check Android -> Java ME mode flags without any live game connection. */
public final class ConfigModeOfflineTest {
    public static void main(String[] args) throws Exception {
        Method build=LegacyBotRuntime.class.getDeclaredMethod("createConfig", String.class,
                String.class,String.class,int.class,int.class,boolean.class,boolean.class);
        build.setAccessible(true);
        for(int mode=0;mode<=3;mode++){
            Config c=(Config)build.invoke(null,"demo","not_real","127.0.0.1",12345,mode,true,false);
            if(c.autoFishing!=(mode==1) || c.autoDiamond!=(mode==2) || c.autoTeacher!=(mode==3))
                throw new AssertionError("Auto flags for mode " + mode);
            if(c.fishFarmIntervalMin!=0 || c.diamondFarmIntervalMin!=0)
                throw new AssertionError("Unexpected background farm in mode " + mode);
            if(c.autoHarvestLove || !c.traderAssist || c.kitchenEnabled)
                throw new AssertionError("Unrequested options changed");
        }
        System.out.println("PASS: JAR mode flags and no surprise Farm transitions in Fish/Diamond modes");
    }
}
