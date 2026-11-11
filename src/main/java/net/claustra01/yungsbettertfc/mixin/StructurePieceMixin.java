package net.claustra01.yungsbettertfc.mixin;

import net.claustra01.yungsbettertfc.access.ConvertedStructurePiece;
import net.claustra01.yungsbettertfc.world.conversion.DirectPlacementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructurePiece.class)
public abstract class StructurePieceMixin {
    @Shadow
    public abstract BoundingBox getBoundingBox();

    @Redirect(
            method = "placeBlock(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/block/state/BlockState;IIILnet/minecraft/world/level/levelgen/structure/BoundingBox;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            remap = false)
    private boolean yungsbettertfc$convertPlacedBlock(
            WorldGenLevel level, BlockPos position, BlockState state, int flags) {
        return level.setBlock(position, yungsbettertfc$convert(level, position, state), flags);
    }

    @Redirect(
            method = "createChest(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ServerLevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            remap = false)
    private boolean yungsbettertfc$convertPlacedChest(
            ServerLevelAccessor level, BlockPos position, BlockState state, int flags) {
        return level.setBlock(position, yungsbettertfc$convert(level, position, state), flags);
    }

    @Unique
    private BlockState yungsbettertfc$convert(
            ServerLevelAccessor level, BlockPos position, BlockState state) {
        if (!((Object) this instanceof ConvertedStructurePiece)) {
            return state;
        }
        BoundingBox box = getBoundingBox();
        return DirectPlacementContext.convert(
                level, new BlockPos(box.minX(), box.minY(), box.minZ()), position, state);
    }
}
