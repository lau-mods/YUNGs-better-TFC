package net.claustra01.yungsbettertfc.mixin.bettermineshafts;

import net.claustra01.yungsbettertfc.access.ConvertedStructurePiece;
import net.claustra01.yungsbettertfc.world.conversion.DirectPlacementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(
        targets = "com.yungnickyoung.minecraft.bettermineshafts.world.generator.pieces.BetterMineshaftPiece",
        remap = false)
public abstract class BetterMineshaftPieceMixin implements ConvertedStructurePiece {

    @Unique
    private static final String SET_BLOCK = "Lnet/minecraft/world/level/WorldGenLevel;setBlock("
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z";

    @Dynamic("Better Mineshafts optional piece method")
    @Redirect(method = "generateLegOrChain", at = @At(value = "INVOKE", target = SET_BLOCK), remap = false)
    private boolean yungsbettertfc$convertLeg(
            WorldGenLevel level, BlockPos position, BlockState state, int flags) {
        return yungsbettertfc$setConverted(level, position, state, flags);
    }

    @Dynamic("Better Mineshafts optional piece method")
    @Redirect(method = "generatePillarDownOrChainUp", at = @At(value = "INVOKE", target = SET_BLOCK), remap = false)
    private boolean yungsbettertfc$convertPillar(
            WorldGenLevel level, BlockPos position, BlockState state, int flags) {
        return yungsbettertfc$setConverted(level, position, state, flags);
    }

    @Dynamic("Better Mineshafts optional column call sites")
    @Redirect(
            method = {"generateLegOrChain", "generatePillarDownOrChainUp(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;IIIILnet/minecraft/world/level/block/state/BlockState;)V"},
            at = @At(value = "INVOKE", target = "Lcom/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/BetterMineshaftPiece;fillColumnBetween(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos$MutableBlockPos;II)V"),
            remap = false)
    private void yungsbettertfc$convertColumn(
            WorldGenLevel level, BlockState state, BlockPos.MutableBlockPos position, int startY, int endY) {
        for (int y = startY; y < endY; y++) {
            yungsbettertfc$setConverted(level, position.setY(y), state, 2);
        }
    }

    @Unique
    private boolean yungsbettertfc$setConverted(
            WorldGenLevel level, BlockPos position, BlockState state, int flags) {
        BoundingBox box = ((StructurePiece) (Object) this).getBoundingBox();
        BlockPos origin = new BlockPos(box.minX(), box.minY(), box.minZ());
        return level.setBlock(position, DirectPlacementContext.convert(level, origin, position, state), flags);
    }
}
