package net.claustra01.yungsbettertfc.world.conversion;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

public final class RockConversions {
    private static final Set<String> RAW_BLOCKS = Set.of("stone", "andesite", "granite", "diorite", "tuff");
    private static final Set<String> POLISHED_BLOCKS =
            Set.of("polished_andesite", "polished_granite", "polished_diorite", "polished_tuff");

    private RockConversions() {}

    public static @Nullable ResourceLocation candidate(String path, String rock) {
        if (RAW_BLOCKS.contains(path)) {
            return rock("raw", rock, "");
        }
        for (String base : RAW_BLOCKS) {
            String suffix = shapeSuffix(path, base, true);
            if (suffix != null) {
                return rock("raw", rock, suffix);
            }
        }
        if (POLISHED_BLOCKS.contains(path)) {
            return rock("smooth", rock, "");
        }
        for (String base : POLISHED_BLOCKS) {
            String suffix = shapeSuffix(path, base, false);
            if (suffix != null) {
                return rock("smooth", rock, suffix);
            }
        }

        return switch (path) {
            case "cobblestone" -> rock("cobble", rock, "");
            case "cobblestone_stairs" -> rock("cobble", rock, "_stairs");
            case "cobblestone_slab" -> rock("cobble", rock, "_slab");
            case "cobblestone_wall" -> rock("cobble", rock, "_wall");
            case "mossy_cobblestone" -> rock("mossy_cobble", rock, "");
            case "mossy_cobblestone_stairs" -> rock("mossy_cobble", rock, "_stairs");
            case "mossy_cobblestone_slab" -> rock("mossy_cobble", rock, "_slab");
            case "mossy_cobblestone_wall" -> rock("mossy_cobble", rock, "_wall");
            case "stone_bricks", "tuff_bricks" -> rock("bricks", rock, "");
            case "stone_brick_stairs", "tuff_brick_stairs" -> rock("bricks", rock, "_stairs");
            case "stone_brick_slab", "tuff_brick_slab" -> rock("bricks", rock, "_slab");
            case "stone_brick_wall", "tuff_brick_wall" -> rock("bricks", rock, "_wall");
            case "mossy_stone_bricks" -> rock("mossy_bricks", rock, "");
            case "mossy_stone_brick_stairs" -> rock("mossy_bricks", rock, "_stairs");
            case "mossy_stone_brick_slab" -> rock("mossy_bricks", rock, "_slab");
            case "mossy_stone_brick_wall" -> rock("mossy_bricks", rock, "_wall");
            case "cracked_stone_bricks" -> rock("cracked_bricks", rock, "");
            case "chiseled_stone_bricks", "chiseled_tuff", "chiseled_tuff_bricks" ->
                    rock("chiseled", rock, "");
            case "smooth_stone" -> rock("smooth", rock, "");
            case "smooth_stone_slab" -> rock("smooth", rock, "_slab");
            case "gravel" -> rock("gravel", rock, "");
            case "stone_button" -> rock("button", rock, "");
            case "stone_pressure_plate" -> rock("pressure_plate", rock, "");
            default -> null;
        };
    }

    public static boolean isHardenable(ResourceLocation id, String rock) {
        return id.equals(rock("raw", rock, ""))
                || id.equals(rock("cobble", rock, ""))
                || id.equals(rock("mossy_cobble", rock, ""));
    }

    public static ResourceLocation hardened(String rock) {
        return rock("hardened", rock, "");
    }

    private static @Nullable String shapeSuffix(String path, String base, boolean includeWall) {
        if (path.equals(base + "_stairs")) {
            return "_stairs";
        }
        if (path.equals(base + "_slab")) {
            return "_slab";
        }
        if (includeWall && path.equals(base + "_wall")) {
            return "_wall";
        }
        return null;
    }

    private static ResourceLocation rock(String family, String rock, String suffix) {
        return ResourceLocation.fromNamespaceAndPath("tfc", "rock/" + family + "/" + rock + suffix);
    }
}
