package net.claustra01.yungsbettertfc.world.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.claustra01.yungsbettertfc.world.conversion.BlockConversions;
import net.claustra01.yungsbettertfc.world.conversion.ConversionContext;
import net.claustra01.yungsbettertfc.world.conversion.DimensionKind;
import net.claustra01.yungsbettertfc.world.conversion.EquipmentConversions;
import net.claustra01.yungsbettertfc.world.conversion.FinalStateRewriter;
import net.claustra01.yungsbettertfc.world.conversion.GeologyDetector;
import net.claustra01.yungsbettertfc.world.conversion.WoodHint;
import net.minecraft.core.BlockPos;
import net.dries007.tfc.common.blocks.devices.FirepitBlock;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class TfcBlockReplacementProcessor extends StructureProcessor {
    public static final TfcBlockReplacementProcessor INSTANCE = new TfcBlockReplacementProcessor();
    public static final MapCodec<TfcBlockReplacementProcessor> CODEC = MapCodec.unit(INSTANCE);

    private TfcBlockReplacementProcessor() {}

    @Override
    protected StructureProcessorType<?> getType() {
        return ModProcessors.TFC_BLOCK_REPLACEMENT.get();
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level,
            BlockPos offset,
            BlockPos pos,
            List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
            List<StructureTemplate.StructureBlockInfo> processedBlockInfos,
            StructurePlaceSettings settings) {
        DimensionKind dimension = DimensionKind.resolve(level);
        if (dimension == DimensionKind.NONE) {
            return processedBlockInfos;
        }

        ConversionContext context;
        WoodHint woodHint = WoodHint.fromOriginalBlocks(originalBlockInfos, dimension);
        if (dimension == DimensionKind.NETHER) {
            context = new ConversionContext(dimension, "basalt", "oxisol", woodHint, false);
        } else {
            GeologyDetector.Geology geology = GeologyDetector.detect(level, offset);
            context = new ConversionContext(dimension, geology.rock(), geology.soil(), woodHint, false);
        }

        Map<BlockPos, BlockState> templateByPosition = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            templateByPosition.put(info.pos(), info.state());
        }

        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>(processedBlockInfos.size());
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            BlockPos below = info.pos().below();
            BlockState templateStateBelow = templateByPosition.get(below);
            boolean belowIsAir = templateStateBelow != null
                    ? templateStateBelow.isAir()
                    : level.getBlockState(below).isAir();
            boolean cactusBelow = templateStateBelow != null
                    ? templateStateBelow.is(Blocks.CACTUS)
                    : level.getBlockState(below).is(Blocks.CACTUS);
            BlockState converted = BlockConversions.convert(
                    info.state(), context.withPlacementBelow(belowIsAir, cactusBelow));
            boolean clearNbt = converted.getBlock() instanceof FirepitBlock
                    && info.state().getBlock() != converted.getBlock();
            result.add(new StructureTemplate.StructureBlockInfo(
                    info.pos(), converted, clearNbt ? null : FinalStateRewriter.rewrite(info.nbt(), context.rock())));
        }
        return result;
    }

    @Override
    public StructureTemplate.StructureEntityInfo processEntity(
            LevelReader level,
            BlockPos pos,
            StructureTemplate.StructureEntityInfo rawEntityInfo,
            StructureTemplate.StructureEntityInfo processedEntityInfo,
            StructurePlaceSettings settings,
            StructureTemplate template) {
        if (DimensionKind.resolve(level) == DimensionKind.NONE) {
            return processedEntityInfo;
        }
        var rewritten = EquipmentConversions.rewriteEntity(processedEntityInfo.nbt);
        return rewritten == processedEntityInfo.nbt
                ? processedEntityInfo
                : new StructureTemplate.StructureEntityInfo(
                        processedEntityInfo.pos, processedEntityInfo.blockPos, rewritten);
    }
}
