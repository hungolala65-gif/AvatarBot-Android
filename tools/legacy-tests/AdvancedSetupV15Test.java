import farm2x.Config;
import com.avatarbot.mobile.legacy.LegacyAdvancedSetup;
import com.avatarbot.mobile.legacy.LegacyBotRuntime;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/** All four Auto pages map to real JAR fields and do not affect other modes. */
public final class AdvancedSetupV15Test {
    public static void main(String[] args) throws Exception {
        Method factory=LegacyBotRuntime.class.getDeclaredMethod("createConfig",
            String.class,String.class,String.class,int.class,int.class,boolean.class,
            boolean.class,int.class,int.class,String.class,int.class,int.class,
            boolean.class,boolean.class,boolean.class,LegacyAdvancedSetup.class);
        factory.setAccessible(true);
        LegacyAdvancedSetup selection=new LegacyAdvancedSetup(199,22,30,3,501,502,5,900);
        for(int mode=0;mode<4;mode++) {
            Config cfg=(Config)factory.invoke(null,"test","secret","127.0.0.1",19128,mode,
                false,false,0,30,"preferred",10,8,true,true,true,selection);
            if(cfg.preferredSeedID!=(mode==0?199:-1) || cfg.targetLandCount!=(mode==0?22:0))
                throw new AssertionError("Farm field leaked to mode "+mode);
            if(cfg.fishingMap!=(mode==1?30:14) || cfg.fishingArea!=(mode==1?3:-1)
               || cfg.fishingBaitID!=(mode==1?501:448) || cfg.fishingRodID!=(mode==1?502:442))
                throw new AssertionError("Fishing field leaked to mode "+mode);
            if(cfg.diamondSellDurationMin!=(mode==2?5:3)
               || cfg.diamondSellIntervalMs!=(mode==2?900:200))
                throw new AssertionError("Diamond field leaked to mode "+mode);
            if(!cfg.noLuongBuy || cfg.autoSell || !"off".equals(cfg.animalBuyMode))
                throw new AssertionError("Unsafe purchases/bulk sales");
        }
        for(int which=0;which<8;which++) {
            int[] values = {-1,0,14,-1,448,442,3,200};
            values[which]=which==7?199:(which==6?0:(which==1?-1:100000));
            try {
                new LegacyAdvancedSetup(values[0],values[1],values[2],values[3],values[4],
                    values[5],values[6],values[7]);
                throw new AssertionError("Invalid field accepted: "+which);
            } catch(IllegalArgumentException expected) { }
        }
        System.out.println("PASS: v0.15 all four modes map actual JAR fields; invalid codes rejected");
    }
}
