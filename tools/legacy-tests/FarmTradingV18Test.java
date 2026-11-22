import farm2x.Config;
import com.avatarbot.mobile.legacy.LegacyAdvancedSetup;
import com.avatarbot.mobile.legacy.LegacyBotRuntime;
import java.lang.reflect.Method;

public final class FarmTradingV18Test {
    static Config mapped(int mode,LegacyAdvancedSetup x) throws Exception {
        Method factory=LegacyBotRuntime.class.getDeclaredMethod("createConfig",
            String.class,String.class,String.class,int.class,int.class,boolean.class,
            boolean.class,int.class,int.class,String.class,int.class,int.class,
            boolean.class,boolean.class,boolean.class,LegacyAdvancedSetup.class);
        factory.setAccessible(true);
        return (Config)factory.invoke(null,"t","p","127.0.0.1",19128,mode,false,false,
            0,30,"preferred",0,-1,false,false,false,x);
    }
    static void reject(String codes,boolean enabled) {
        try {
            new LegacyAdvancedSetup(-1,0,14,-1,448,442,3,200,enabled,codes,50,false,false,50);
            throw new AssertionError("Bad whitelist accepted: "+codes);
        } catch(IllegalArgumentException expected) { }
    }
    public static void main(String[] args) throws Exception {
        LegacyAdvancedSetup defaults=new LegacyAdvancedSetup();
        Config base=mapped(0,defaults);
        if(base.autoSell || !"off".equals(base.animalBuyMode) || base.autoStarUpgrade || !base.noLuongBuy)
            throw new AssertionError("Default farm actions should not sell/buy/upgrade");
        LegacyAdvancedSetup configured=new LegacyAdvancedSetup(-1,15,14,-1,448,442,3,200,
            true,"123, 456",45,true,true,12);
        Config farm=mapped(0,configured);
        if(!farm.autoSell || farm.sellProductIDs.length!=2 || farm.sellProductIDs[0]!=123 ||
           farm.sellProductIDs[1]!=456 || farm.sellQuantityThreshold!=45 ||
           !"rebuy_sold".equals(farm.animalBuyMode) || !farm.autoStarUpgrade ||
           farm.starTargetLevel!=12 || !farm.noLuongBuy)
            throw new AssertionError("Farm mapped incorrectly");
        for(int mode=1;mode<4;mode++) {
            Config other=mapped(mode,configured);
            if(other.autoSell || other.sellProductIDs.length!=0 ||
               !"off".equals(other.animalBuyMode) || other.autoStarUpgrade)
                throw new AssertionError("Farm trading flags leak into other Auto mode "+mode);
        }
        reject("",true); reject("-1",true); reject("0",true);
        reject("1,2,2",true); reject("1,",true); reject("70000",true);
        reject("1,2,three",true); reject("1,2,",false);
        System.out.println("PASS: v0.18 explicit farm trading, no wildcard, mode isolation, safe defaults");
    }
}
