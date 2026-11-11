package net.claustra01.yungsbettertfc.world.conversion;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

public final class SoilConversions {
    private SoilConversions() {}

    public static @Nullable ResourceLocation candidate(String path, String soil) {
        ResourceLocation sandstone = sandstone(path);
        if (sandstone != null) {
            return sandstone;
        }

        return switch (path) {
            case "dirt" -> tfc("dirt/" + soil);
            case "coarse_dirt" -> tfc("coarse_dirt/" + soil);
            case "grass_block" -> tfc("grass/" + soil);
            case "rooted_dirt" -> tfc("rooted_dirt/" + soil);
            case "farmland" -> tfc("farmland/" + soil);
            case "dirt_path" -> tfc("grass_path/" + soil);
            case "sand" -> tfc("sand/yellow");
            case "red_sand" -> tfc("sand/red");
            default -> null;
        };
    }

    private static @Nullable ResourceLocation sandstone(String path) {
        String color = path.contains("red_sandstone") ? "red" : "yellow";
        String normalized = path.replace("red_sandstone", "sandstone");
        return switch (normalized) {
            case "sandstone" -> sandstone("raw_sandstone", color, "");
            case "sandstone_stairs" -> sandstone("raw_sandstone", color, "_stairs");
            case "sandstone_slab" -> sandstone("raw_sandstone", color, "_slab");
            case "sandstone_wall" -> sandstone("raw_sandstone", color, "_wall");
            case "cut_sandstone", "chiseled_sandstone" -> sandstone("cut_sandstone", color, "");
            case "cut_sandstone_slab" -> sandstone("cut_sandstone", color, "_slab");
            case "smooth_sandstone" -> sandstone("smooth_sandstone", color, "");
            case "smooth_sandstone_stairs" -> sandstone("smooth_sandstone", color, "_stairs");
            case "smooth_sandstone_slab" -> sandstone("smooth_sandstone", color, "_slab");
            default -> null;
        };
    }

    private static ResourceLocation sandstone(String family, String color, String suffix) {
        return tfc(family + "/" + color + suffix);
    }

    private static ResourceLocation tfc(String path) {
        return ResourceLocation.fromNamespaceAndPath("tfc", path);
    }
}
