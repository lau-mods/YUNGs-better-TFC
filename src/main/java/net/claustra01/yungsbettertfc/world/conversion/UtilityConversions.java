package net.claustra01.yungsbettertfc.world.conversion;

import net.dries007.tfc.common.blocks.devices.FirepitBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class UtilityConversions {
    private static final ResourceLocation FIREPIT = ResourceLocation.fromNamespaceAndPath("tfc", "firepit");

    private UtilityConversions() {}

    public static ResourceLocation candidate(String path) {
        return isFirepitInput(path) ? FIREPIT : null;
    }

    public static boolean isFirepitInput(String path) {
        return path.equals("furnace") || path.equals("blast_furnace") || path.equals("smoker");
    }

    public static BlockState applyFirepitAxis(BlockState original, BlockState converted) {
        Direction.Axis axis = original.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis();
        return converted.setValue(FirepitBlock.AXIS, axis);
    }
}
