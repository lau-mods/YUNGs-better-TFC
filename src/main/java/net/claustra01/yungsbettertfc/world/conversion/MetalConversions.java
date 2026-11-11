package net.claustra01.yungsbettertfc.world.conversion;

import net.minecraft.resources.ResourceLocation;

public final class MetalConversions {
    private MetalConversions() {}

    public static ResourceLocation candidate(String path, DimensionKind dimension) {
        if (path.equals("coal_block")) {
            return dimension == DimensionKind.NETHER
                    ? ResourceLocation.fromNamespaceAndPath("tfc", "bituminous_coal")
                    : null;
        }
        String output = switch (path) {
            case "iron_bars" -> "metal/bars/wrought_iron";
            case "chain" -> "metal/chain/wrought_iron";
            case "iron_block" -> "metal/block/wrought_iron";
            case "iron_trapdoor" -> "metal/trapdoor/wrought_iron";
            case "gold_block", "raw_gold_block" -> "metal/block/gold";
            case "copper_block", "cut_copper" -> "metal/block/copper";
            case "cut_copper_slab" -> "metal/block/copper_slab";
            case "cut_copper_stairs" -> "metal/block/copper_stairs";
            case "exposed_copper", "exposed_cut_copper" -> "metal/exposed_block/copper";
            case "exposed_cut_copper_slab" -> "metal/exposed_block/copper_slab";
            case "exposed_cut_copper_stairs" -> "metal/exposed_block/copper_stairs";
            case "weathered_copper", "weathered_cut_copper" -> "metal/weathered_block/copper";
            case "weathered_cut_copper_slab" -> "metal/weathered_block/copper_slab";
            case "weathered_cut_copper_stairs" -> "metal/weathered_block/copper_stairs";
            case "oxidized_copper", "oxidized_cut_copper", "waxed_oxidized_cut_copper" ->
                    "metal/oxidized_block/copper";
            case "oxidized_cut_copper_slab", "waxed_oxidized_cut_copper_slab" ->
                    "metal/oxidized_block/copper_slab";
            case "oxidized_cut_copper_stairs", "waxed_oxidized_cut_copper_stairs" ->
                    "metal/oxidized_block/copper_stairs";
            default -> null;
        };
        return output == null ? null : ResourceLocation.fromNamespaceAndPath("tfc", output);
    }
}
