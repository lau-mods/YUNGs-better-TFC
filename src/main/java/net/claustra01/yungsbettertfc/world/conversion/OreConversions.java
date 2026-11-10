package net.claustra01.yungsbettertfc.world.conversion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class OreConversions {
    private static final String GRANITE = "granite";

    private OreConversions() {}

    public static ResourceLocation candidate(String path, String rock) {
        return switch (path) {
            case "coal_ore", "deepslate_coal_ore" -> firstExistingBlock("lignite", "bituminous_coal");
            case "iron_ore", "deepslate_iron_ore" ->
                    firstExistingOre(rock, "normal_hematite", "normal_magnetite", "normal_limonite");
            case "copper_ore", "deepslate_copper_ore" -> firstExistingOre(rock, "normal_native_copper");
            case "gold_ore", "deepslate_gold_ore" -> firstExistingOre(rock, "normal_native_gold");
            case "lapis_ore", "deepslate_lapis_ore" -> firstExistingOre(rock, "lapis_lazuli");
            case "diamond_ore", "deepslate_diamond_ore" -> firstExistingOre(rock, "diamond");
            case "emerald_ore", "deepslate_emerald_ore" -> firstExistingOre(rock, "emerald");
            case "redstone_ore", "deepslate_redstone_ore" -> firstExistingOre(rock, "cinnabar");
            default -> null;
        };
    }

    private static ResourceLocation firstExistingBlock(String... paths) {
        for (String path : paths) {
            ResourceLocation candidate = tfc(path);
            if (BuiltInRegistries.BLOCK.containsKey(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static ResourceLocation firstExistingOre(String rock, String... ores) {
        for (String ore : ores) {
            ResourceLocation candidate = tfc("ore/" + ore + "/" + rock);
            if (BuiltInRegistries.BLOCK.containsKey(candidate)) {
                return candidate;
            }
            ResourceLocation fallback = tfc("ore/" + ore + "/" + GRANITE);
            if (BuiltInRegistries.BLOCK.containsKey(fallback)) {
                return fallback;
            }
        }
        return null;
    }

    private static ResourceLocation tfc(String path) {
        return ResourceLocation.fromNamespaceAndPath("tfc", path);
    }
}
