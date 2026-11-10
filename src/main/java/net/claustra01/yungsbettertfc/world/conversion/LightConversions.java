package net.claustra01.yungsbettertfc.world.conversion;

import net.minecraft.resources.ResourceLocation;

public final class LightConversions {
    private LightConversions() {}

    public static ResourceLocation candidate(String path) {
        if (path.equals("torch") || path.equals("wall_torch")
                || path.equals("candle") || path.equals("candle_cake")) {
            return ResourceLocation.fromNamespaceAndPath("tfc", path);
        }
        if (path.endsWith("_candle_cake")) {
            String color = path.substring(0, path.length() - "_candle_cake".length());
            return ResourceLocation.fromNamespaceAndPath("tfc", "candle_cake/" + color);
        }
        if (path.endsWith("_candle")) {
            String color = path.substring(0, path.length() - "_candle".length());
            return ResourceLocation.fromNamespaceAndPath("tfc", "candle/" + color);
        }
        return null;
    }
}
