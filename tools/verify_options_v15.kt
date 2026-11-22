package com.avatarbot.mobile.data
import org.json.JSONObject
fun main() {
    val original = AutoOptions(farm=true, fishing=true, teacher=true, diamond=true,
        plant=false, dailyQuest=true, farmCheckin=true, loopDelaySeconds=120,
        fishingRounds=20, teacherArea=9, diamondSellKCX=true,
        preferredSeedId=315, targetLandCount=28, fishingMap=36, fishingArea=2,
        fishingBaitId=580, fishingRodId=590,
        diamondSellDurationMin=8, diamondSellIntervalMs=900,
        farmAutoSell=true, farmSellProductIds="123,456", farmSellThreshold=45,
        farmRebuySold=true, farmStarUpgrade=true, farmStarTargetLevel=12)
    check(AutoOptions.fromJson(original.toJson()) == original) { "New settings not persisted" }
    val legacy = AutoOptions.fromJson(JSONObject().put("farm", true).put("fishing", false))
    check(legacy.fishingMap == 14 && legacy.fishingBaitId == 448 && legacy.fishingRodId == 442)
    check(legacy.diamondSellDurationMin == 3 && legacy.diamondSellIntervalMs == 200)
    check(!legacy.diamondSellKCX && !legacy.diamondSellNHB && !legacy.animalSellLate)
    check(!legacy.farmAutoSell && !legacy.farmRebuySold && !legacy.farmStarUpgrade)
    val invalid = JSONObject().put("fishingMap",-100).put("fishingBaitId",900000)
        .put("teacherArea",999).put("diamondSellIntervalMs",-1)
    val normalized = AutoOptions.fromJson(invalid)
    check(normalized.fishingMap == 0 && normalized.fishingBaitId == 65535)
    check(normalized.teacherArea == 99 && normalized.diamondSellIntervalMs == 200)
    val otherAccount = AutoOptions()
    val edited = original.copy(fishingRounds=10)
    check(edited.fishingRounds==10 && otherAccount.fishingRounds==0)
    check(edited.preferredSeedId==315 && otherAccount.preferredSeedId == -1)
    println("PASS: v0.18 AutoOptions round-trip including farm sale whitelist and per-account controls")
}
