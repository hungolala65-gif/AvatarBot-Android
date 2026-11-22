import com.avatarbot.mobile.legacy.LegacyBotRuntime;
import com.avatarbot.mobile.legacy.LegacyFarmCompatibility;
import farm2x.Config;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/** Check the exact Android -> JAR configuration values; offline only. */
public final class AdvancedOptionsOfflineTest {
    public static void main(String[] args) throws Exception {
        Method legacy=LegacyBotRuntime.class.getDeclaredMethod("createConfig",
            String.class, String.class, String.class, int.class, int.class,
            boolean.class, boolean.class);
        legacy.setAccessible(true);
        for(int mode=0;mode<4;mode++) {
            Config c=(Config)legacy.invoke(null,"demo","secret","127.0.0.1",19128,mode,false,false);
            if(c.diamondSellKCX || c.diamondSellNHB || c.autoSell || c.autoStarUpgrade || !c.noLuongBuy || !"off".equals(c.animalBuyMode) || !"".equals(c.animalSellMode))
                throw new AssertionError("Unsafe default for mode="+mode);
            if(c.fishingRounds!=0 || c.teacherArea!=-1)
                throw new AssertionError("Unexpected advanced defaults");
        }
        Method advanced=LegacyBotRuntime.class.getDeclaredMethod("createConfig",
            String.class, String.class, String.class, int.class, int.class,
            boolean.class, boolean.class, int.class, int.class, String.class,
            int.class, int.class, boolean.class, boolean.class, boolean.class);
        advanced.setAccessible(true);
        for(int mode=0;mode<4;mode++) {
            Config c=(Config)advanced.invoke(null,"demo","secret","127.0.0.1",19128,mode,
                false,false,0,15,"preferred",20,7,true,false,false);
            if(c.fishingRounds!=(mode==1?20:0) || c.teacherArea!=(mode==3?7:-1) ||
                c.diamondSellKCX!=(mode==2) || c.diamondSellNHB)
                throw new AssertionError("Cross-mode settings leak mode="+mode);
        }
        Config diamonds=(Config)advanced.invoke(null,"demo","secret","127.0.0.1",19128,2,
            false,false,0,15,"preferred",0,-1,true,true,true);
        if(!diamonds.diamondSellKCX || !diamonds.diamondSellNHB || !"".equals(diamonds.animalSellMode))
            throw new AssertionError("Diamond flags or cross-mode animal sale issue");
        Config farmSale=(Config)advanced.invoke(null,"demo","secret","127.0.0.1",19128,0,
            false,false,0,15,"preferred",0,-1,false,false,true);
        if(!"late".equals(farmSale.animalSellMode)) throw new AssertionError("Explicit late animal sale not mapped");
        for (int[] bad: new int[][]{{-1,-1},{101,-1},{0,-2},{0,100}}) {
            try {
                advanced.invoke(null,"demo","secret","127.0.0.1",19128,0,
                    false,false,0,15,"preferred",bad[0],bad[1],false,false,false);
                throw new AssertionError("Invalid options accepted");
            } catch(InvocationTargetException e) {
                if(!(e.getCause() instanceof IllegalArgumentException)) throw e;
            }
        }
        if(!LegacyFarmCompatibility.isCompatible(true,true,true,false) ||
           LegacyFarmCompatibility.isCompatible(false,true,true,false) ||
           LegacyFarmCompatibility.isCompatible(true,false,true,false) ||
           LegacyFarmCompatibility.isCompatible(true,true,false,false) ||
           LegacyFarmCompatibility.isCompatible(true,true,true,true))
           throw new AssertionError("Farm policy invalid");
        System.out.println("PASS: advanced JAR mode config, opt-in sales, safe defaults, and unsupported Farm flags");
    }
}
