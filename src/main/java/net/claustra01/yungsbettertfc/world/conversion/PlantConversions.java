package net.claustra01.yungsbettertfc.world.conversion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class PlantConversions {
    private PlantConversions() {}

    public static ResourceLocation candidate(String path, BlockState original, boolean cactusBelow) {
        if (path.equals("kelp") || path.equals("kelp_plant")) {
            return minecraft("water");
        }
        if (path.equals("tall_seagrass")
                && original.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return minecraft("water");
        }
        if (path.equals("seagrass") || path.equals("tall_seagrass")) {
            return tfc("plant/eel_grass");
        }
        if (path.equals("sea_pickle")) {
            return tfc("sea_pickle");
        }
        if (path.equals("vine")) {
            return tfc("plant/ivy");
        }
        if (path.equals("cactus")) {
            return cactusBelow ? minecraft("air") : tfc("plant/silken_pincushion_cactus");
        }
        if (path.equals("dead_bush")) {
            return tfc("plant/dead_bush");
        }
        if (path.startsWith("potted_")) {
            ResourceLocation candidate = tfc("plant/potted/" + path.substring("potted_".length()));
            return BuiltInRegistries.BLOCK.containsKey(candidate) ? candidate : null;
        }
        return null;
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    private static ResourceLocation tfc(String path) {
        return ResourceLocation.fromNamespaceAndPath("tfc", path);
    }
}
