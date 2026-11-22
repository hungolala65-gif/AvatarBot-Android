package com.avatarbot.mobile.legacy;

import farm2x.Config;
import java.util.HashSet;
import java.util.Set;

/** Existing JAR fields only; conservative defaults avoid purchases and unrequested sales. */
public final class LegacyAdvancedSetup {
    public final int preferredSeedId, targetLandCount;
    public final int fishingMap, fishingArea, fishingBaitId, fishingRodId;
    public final int diamondSellDurationMin, diamondSellIntervalMs;
    public final boolean farmAutoSell, farmRebuySold, farmStarUpgrade;
    public final String farmSellProductIds;
    public final int farmSellThreshold, farmStarTargetLevel;

    public LegacyAdvancedSetup() { this(-1,0,14,-1,448,442,3,200); }
    public LegacyAdvancedSetup(int seed, int land, int map, int area, int bait, int rod,
                               int duration, int interval) {
        this(seed,land,map,area,bait,rod,duration,interval,false,"",50,false,false,50);
    }
    public LegacyAdvancedSetup(int seed, int land, int map, int area, int bait, int rod,
                               int duration, int interval, boolean autoSell, String productIds,
                               int threshold, boolean rebuy, boolean starUpgrade, int starTarget) {
        preferredSeedId=seed; targetLandCount=land;
        fishingMap=map; fishingArea=area; fishingBaitId=bait; fishingRodId=rod;
        diamondSellDurationMin=duration; diamondSellIntervalMs=interval;
        farmAutoSell=autoSell; farmSellProductIds=productIds==null?"":productIds.trim();
        farmSellThreshold=threshold; farmRebuySold=rebuy;
        farmStarUpgrade=starUpgrade; farmStarTargetLevel=starTarget;
        validate();
    }
    private static void between(String name, int value, int min, int max) {
        if (value<min || value>max) throw new IllegalArgumentException(name+" out of supported range");
    }
    /** No wildcard, duplicate identifiers, negative codes, or unbounded product lists. */
    public static int[] parseSellWhitelist(String value) {
        if (value==null || value.trim().isEmpty()) return new int[0];
        String[] fields=value.split(",",-1);
        if(fields.length>16) throw new IllegalArgumentException("Too many product IDs");
        int[] ids=new int[fields.length];
        Set<Integer> seen=new HashSet<Integer>();
        for(int i=0;i<fields.length;i++) {
            String code=fields[i].trim();
            if(!code.matches("[0-9]{1,5}")) throw new IllegalArgumentException("Invalid product ID");
            int parsed=Integer.parseInt(code);
            between("product ID",parsed,1,65535);
            if(!seen.add(parsed)) throw new IllegalArgumentException("Duplicate product ID");
            ids[i]=parsed;
        }
        return ids;
    }
    public void validate() {
        between("preferredSeedId",preferredSeedId,-1,65535);
        between("targetLandCount",targetLandCount,0,100);
        between("fishingMap",fishingMap,0,999);
        between("fishingArea",fishingArea,-1,99);
        between("fishingBaitId",fishingBaitId,0,65535);
        between("fishingRodId",fishingRodId,0,65535);
        between("diamondSellDurationMin",diamondSellDurationMin,1,30);
        between("diamondSellIntervalMs",diamondSellIntervalMs,200,5000);
        between("farmSellThreshold",farmSellThreshold,1,99999);
        between("farmStarTargetLevel",farmStarTargetLevel,1,100);
        int[] whitelist=parseSellWhitelist(farmSellProductIds);
        if(farmAutoSell && whitelist.length==0)
            throw new IllegalArgumentException("Auto sell requires explicit product IDs");
    }
    /** Explicit opt-in only. No Lượng purchases are permitted by the runtime. */
    public void applyFarm(Config cfg) {
        cfg.autoSell=farmAutoSell;
        cfg.sellProductIDs=farmAutoSell ? parseSellWhitelist(farmSellProductIds) : new int[0];
        cfg.sellQuantityThreshold=farmAutoSell ? farmSellThreshold : 0;
        cfg.sellCount=0;
        cfg.animalBuyMode=farmRebuySold ? "rebuy_sold" : "off";
        cfg.preferredAnimalID=0; cfg.preferredPondAnimalID=0;
        cfg.autoStarUpgrade=farmStarUpgrade;
        cfg.starTargetLevel=farmStarTargetLevel;
    }
}
