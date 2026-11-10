package net.claustra01.yungsbettertfc.world.conversion;

import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockConversions {
    private static final Set<String> NETHER_KEEP = Set.of(
            "blackstone",
            "polished_blackstone",
            "basalt",
            "polished_basalt",
            "soul_sand",
            "soul_soil",
            "nether_gold_ore",
            "nether_quartz_ore",
            "ancient_debris");

    private BlockConversions() {}

    public static BlockState convert(BlockState original, ConversionContext context) {
        if (context.dimension() == DimensionKind.NONE || original.isAir()) {
            return original;
        }

        ResourceLocation originalId = BuiltInRegistries.BLOCK.getKey(original.getBlock());
        if (!originalId.getNamespace().equals("minecraft")) {
            return original;
        }

        String path = originalId.getPath();
        if (path.startsWith("infested_")) {
            path = path.substring("infested_".length());
        }
        if (NETHER_KEEP.contains(path)) {
            return original;
        }

        if (context.dimension() == DimensionKind.NETHER && path.equals("netherrack")) {
            return context.belowIsAir()
                    ? RegistryLookup.stateOrOriginal(
                            ResourceLocation.fromNamespaceAndPath("tfcoverdims", "hardened_netherrack"), original)
                    : original;
        }

        ResourceLocation candidate = OreConversions.candidate(path, context.rock());
        if (candidate == null) {
            candidate = RockConversions.candidate(path, context.rock());
        }
        if (candidate == null) {
            candidate = SoilConversions.candidate(path, context.soil());
        }
        if (candidate == null) {
            candidate = WoodConversions.candidate(path, context);
        }
        if (candidate == null) {
            candidate = UtilityConversions.candidate(path);
        }
        if (candidate == null) {
            candidate = LightConversions.candidate(path);
        }
        if (candidate == null) {
            candidate = MetalConversions.candidate(path, context.dimension());
        }
        if (candidate == null) {
            candidate = PlantConversions.candidate(path, original, context.cactusBelow());
        }
        if (candidate == null) {
            return original;
        }
        if (context.belowIsAir() && RockConversions.isHardenable(candidate, context.rock())) {
            candidate = RockConversions.hardened(context.rock());
        }
        BlockState converted = RegistryLookup.stateOrOriginal(candidate, original);
        return UtilityConversions.isFirepitInput(path)
                ? UtilityConversions.applyFirepitAxis(original, converted)
                : converted;
    }

}
