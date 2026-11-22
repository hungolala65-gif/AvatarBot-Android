package com.avatarbot.mobile.legacy;

/** Explicit, conservative mapping for old Android toggles absent from JAR Config.
 *  No individual harvest/water/animal/starfruit switches exist in Bot2X-SuperLite.jar.
 *  Reject unsupported saved combinations instead of silently running the opposite action.
 */
public final class LegacyFarmCompatibility {
    private LegacyFarmCompatibility() { }
    public static boolean isCompatible(boolean harvest, boolean water, boolean animals, boolean starfruit) {
        return harvest && water && animals && !starfruit;
    }
    public static String explanation() {
        return "JAR không cho tắt riêng Thu hoạch/Tưới/Vật nuôi hoặc bật Thu khế. " +
            "Chọn Đặt về mặc định JAR trong Thiết lập trước khi chạy Farm.";
    }
}
