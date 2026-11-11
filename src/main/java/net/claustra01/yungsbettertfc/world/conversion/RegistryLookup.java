package net.claustra01.yungsbettertfc.world.conversion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public final class RegistryLookup {
    private RegistryLookup() {}

    public static BlockState stateOrOriginal(ResourceLocation id, BlockState original) {
        return BuiltInRegistries.BLOCK.getOptional(id)
                .map(block -> StateCopier.copySharedProperties(original, block.defaultBlockState()))
                .orElse(original);
    }
}
